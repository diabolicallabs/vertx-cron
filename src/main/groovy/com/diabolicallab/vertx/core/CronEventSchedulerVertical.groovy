package com.diabolicallab.vertx.core
/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.quartz.CronExpression
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.platform.Verticle
import groovy.json.JsonOutput
import org.vertx.java.core.logging.Logger
import org.vertx.java.core.shareddata.ConcurrentSharedMap

class CronEventSchedulerVertical extends Verticle {

    def start() {

        EventBus eb = vertx.eventBus
        Logger logger = container.logger

        if (!container.config.address_base) throw new IllegalArgumentException('The "address_base" property must be set in the configuration')

        String create_address = "${container.config.address_base}.schedule"
        String cancel_address = "${container.config.address_base}.cancel"

        String scheduler_map_name = "${container.config.address_base}.map"
        String map_add_address = "${container.config.address_base}.map.add"
        String map_remove_address = "${container.config.address_base}.map.remove"


        logger.debug "registering cron create address of ${create_address}"
        eb.registerHandler(create_address) { message ->

            logger.debug "${create_address} received message of: ${message.body}"

            def cron_expression = message?.body?.cron_expression
            def repeat = message?.body?.repeat
            if (repeat == null) repeat = true
            def scheduled_address = message?.body?.address
            def result_address = message?.body?.result_address
            def scheduled_action = message?.body?.action ?: 'send'
            Object scheduled_message = message?.body?.message
            def timezone_name = container.config?.timezone_name

            List<String> error_messages = []

            if (!cron_expression) error_messages.add 'cron_expression must be specified'
            if (!scheduled_address) error_messages.add 'address must be specified'

            if (cron_expression && !(cron_expression instanceof String)) error_messages.add 'cron_expression must be a string'
            if (scheduled_address && !(scheduled_address instanceof String)) error_messages.add 'address must be a string'
            if (repeat && !(repeat instanceof Boolean)) error_messages.add 'repeat must be true or false'
            if (result_address && !(result_address instanceof String)) error_messages.add 'result_address must be a string'
            if (scheduled_action instanceof String) {
                if (!['send','publish'].contains(scheduled_action)) error_messages.add 'action must be "send" or "publish"'
            } else error_messages.add 'action must be a string'
            if (timezone_name && !TimeZone.getAvailableIDs().contains(timezone_name)) {
                error_messages.add "${timezone_name} is not a valid timezone.".toString()
            }

            if (error_messages) {
                String error_message = error_messages.join(', ')
                message.reply([status: 'error', message: error_message])
                return
            }

            try {
                CronExpression.validateExpression(cron_expression)
            } catch (e) {
                message.reply([status: 'error', message: e.getMessage()])
                return
            }

            CronExpression cron = new CronExpression(cron_expression)
            if (timezone_name) {
                logger.debug "using ${timezone_name} timezone"
                cron.setTimeZone(TimeZone.getTimeZone(timezone_name))
            }

            String scheduler_id = UUID.randomUUID().toString()

            Closure schedule
            schedule = {
                Date now = new Date()
                Date next_run = cron.getNextValidTimeAfter(now)
                long delay = next_run.getTime() - now.getTime()

                Closure local_handler = { timer_id ->
                    if (scheduled_action.toLowerCase() == 'send') {
                        logger.debug "sending ${scheduled_address} a message of ${scheduled_message}"
                        eb.send(scheduled_address, scheduled_message) { result ->
                            if (result_address) {
                                eb.send(result_address, result.body)
                            }
                        }
                    } else {
                        logger.debug "sending ${scheduled_address} a message of ${scheduled_message}"
                        eb.publish(scheduled_address, scheduled_message)
                    }
                    if (repeat) schedule()
                    else eb.publish(map_remove_address, scheduler_id)
                }

                long timer_id = vertx.setTimer(delay, local_handler)

                logger.debug "scheduled next message for ${scheduled_address} at ${next_run}"

                eb.publish(map_add_address, [scheduler_id: scheduler_id, timer_id: timer_id])

                message.reply([status: 'ok', scheduler_id: scheduler_id, next_run_time: JsonOutput.toJson(next_run)])
            }

            schedule()
        }

        logger.debug "registering cron cancel address of ${cancel_address}"
        eb.registerHandler(cancel_address) { message ->

            logger.debug "${create_address} received message of: ${message.body}"
            if (!message.body || !(message.body instanceof String)) {
                message.reply([status: 'error', message: 'The message must be a string representing the scheduler_id returned by the create handler'])
                return
            }

            ConcurrentSharedMap scheduler_id_map = vertx.sharedData.getMap(scheduler_map_name)
            if (!scheduler_id_map.containsKey(message.body)) {
                message.reply([status: 'error', message: 'The scheduler_id was not found'])
                return
            }

            vertx.cancelTimer(scheduler_id_map[message.body])

            eb.publish(map_remove_address, message.body)

            message.reply([status: 'ok'])
        }

        // Since in Vertx 2.x, shared maps are not shared across instances, we will publish to this address so that verticals deployed on other vertx instances will have the same information
        logger.debug "registering map add address of ${map_add_address}"
        eb.registerHandler(map_add_address) { message ->

            logger.debug "${map_add_address} received: ${message.body}"
            def scheduler_id = message.body.scheduler_id
            def timer_id = message.body.timer_id

            List<String> error_messages = []

            if (!scheduler_id || !(scheduler_id instanceof String)) error_messages.add 'scheduler_id must be specified as a string'
            if (!timer_id || !(timer_id instanceof Integer || (timer_id instanceof Long))) error_messages.add 'timer_id must be specified as a long'

            if (error_messages) {
                String error_message = error_messages.join(', ')
                logger.error "${map_add_address} ${error_message}"
                message.reply([status: 'error', message: error_message])
                return
            }

            ConcurrentSharedMap scheduler_id_map = vertx.sharedData.getMap(scheduler_map_name)
            scheduler_id_map[scheduler_id] = timer_id

            message.reply([status: 'ok'])
        }

        // Since in Vertx 2.x, shared maps are not shared across instances, we will publish to this address so that verticals deployed on other vertx instances will have the same information
        logger.debug "registering map remove address of ${map_remove_address}"
        eb.registerHandler(map_remove_address) { message ->

            logger.debug "${map_remove_address} received: ${message.body}"

            if (!message.body || !(message.body instanceof String)) {
                def error_message = "message must be the scheduler_id as a string"
                logger.error "${map_remove_address} ${error_message}"
                message.reply([status: 'error', message: error_message])
                return
            }

            ConcurrentSharedMap scheduler_id_map = vertx.sharedData.getMap(scheduler_map_name)
            if (scheduler_id_map.containsKey(message.body)) scheduler_id_map.remove(message.body)
            else logger.warn "${map_remove_address} scheduler_id ${message.body} was not found. Nothing removed."

            message.reply([status: 'ok'])
        }
    }

    def stop() {

        // Make sure our shared map gets removed if this Vertical is undeployed
        if (container.config.address_base) {
            String scheduler_map_name = "${container.config.address_base}.map"
            vertx.sharedData.removeMap(scheduler_map_name)
            container.logger.debug "${scheduler_map_name} removed"
        }
    }
}

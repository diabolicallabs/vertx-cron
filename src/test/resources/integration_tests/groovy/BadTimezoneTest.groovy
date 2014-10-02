package integration_tests.groovy

import static org.vertx.testtools.VertxAssert.*
import org.vertx.groovy.testtools.VertxTests;

def test5SecondNoRepeat() {

    def scheduled_address = "address_${new Date().format('yyyyMMddhhmmssSSS')}".toString()

    vertx.eventBus.registerHandler(scheduled_address) { message ->
        container.logger.info "Handler message: ${message.body}"
        assertEquals('goose', message.body)
        message.reply(null)
        testComplete()
    }

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: scheduled_address,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('ok', result.body.status)
        assertNotNull(result.body.scheduler_id)
        assertNotNull(result.body.next_run_time)
    }
}
def testNullEvent() {

    def scheduled_address = "address_${new Date().format('yyyyMMddhhmmssSSS')}".toString()

    vertx.eventBus.registerHandler(scheduled_address) { message ->
        container.logger.info "Handler message: ${message.body}"
        assertNull(message.body)
        message.reply(null)
        testComplete()
    }

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: scheduled_address
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('ok', result.body.status)
        assertNotNull(result.body.scheduler_id)
        assertNotNull(result.body.next_run_time)
    }
}
def test5SecondNoRepeatPublish() {

    def scheduled_address = "address_${new Date().format('yyyyMMddhhmmssSSS')}".toString()

    def hit1 = false
    def hit2 = false

    vertx.eventBus.registerHandler(scheduled_address) { message ->
        container.logger.info "Handler 1 message: ${message.body}"
        hit1 = true
    }
    vertx.eventBus.registerHandler(scheduled_address) { message ->
        container.logger.info "Handler 2 message: ${message.body}"
        hit2 = true
    }

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            action: 'publish',
            address: scheduled_address,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('ok', result.body.status)
        assertNotNull(result.body.scheduler_id)
        assertNotNull(result.body.next_run_time)
        vertx.setTimer(1000 * 7) { timer_id ->
            assertTrue(hit1)
            assertTrue(hit2)
            testComplete()
        }
    }
}
def testInvalidCron() {

    def event = [
            cron_expression: '*/5 squid * * * ?',
            repeat: false,
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('Illegal characters'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testCronMissing() {

    def event = [
            repeat: true,
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('cron_expression must be specified'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testCronBadType() {

    def event = [
            cron_expression: 5,
            repeat: false,
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('cron_expression must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testInvalidRepeat() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: 'rat',
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('repeat must be true or false'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testMissingAddress() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('address must be specified'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testAddressInvalidType() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 777,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('address must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testResultAddressInvalidType() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'rat',
            result_address: 88,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('result_address must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testActionInvalidType() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'rat',
            result_address: 'squid',
            action: 23,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('action must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testActionInvalidValue() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'rat',
            result_address: 'squid',
            action: 'cry',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('action must be "send" or "publish"'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testCancelWrongIdType() {

    vertx.eventBus.send('cron.test.cancel', 345) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('message must be a string'))
        testComplete()
    }
}
def testCancelNoId() {

    vertx.eventBus.send('cron.test.cancel', UUID.randomUUID().toString()) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('scheduler_id was not found'))
        testComplete()
    }
}
def testCancel() {

    def scheduled_address = "address_${new Date().format('yyyyMMddhhmmssSSS')}".toString()
    String scheduler_id
    def times_called = 0

    vertx.eventBus.registerHandler(scheduled_address) { message ->
        container.logger.info "Handler message: ${message.body}"
        assertEquals('polecat', message.body)
        message.reply(null)
        if (++times_called == 2) {
            vertx.eventBus.send('cron.test.cancel', scheduler_id) { result ->
                container.logger.info "Cancel message: ${message.body}"
                assertEquals('ok', result.body.status)
                vertx.setTimer(1000 * 5) { timer_id ->
                    assertEquals(2, times_called)
                    testComplete()
                }
            }
        }
    }

    def event = [
            cron_expression: '*/3 * * * * ?',
            repeat: true,
            address: scheduled_address,
            message: 'polecat'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('ok', result.body.status)
        assertNotNull(result.body.scheduler_id)
        assertNotNull(result.body.next_run_time)
        scheduler_id = result.body.scheduler_id
    }
}
def testAddTimerId() {

    vertx.eventBus.send('cron.test.map.add', [scheduler_id: UUID.randomUUID().toString(), timer_id: 7]) { result ->
        container.logger.info "map add result: ${result.body}"
        assertEquals('ok', result.body.status)
        testComplete()
    }
}
def testAddTimerIdMissing() {

    vertx.eventBus.send('cron.test.map.add', [scheduler_id: UUID.randomUUID().toString()]) { result ->
        container.logger.info "map add result: ${result.body}"
        assertEquals('error', result.body.status)
        assertEquals('timer_id must be specified as a long', result.body.message)
        testComplete()
    }
}
def testAddTimerIdBadType() {

    vertx.eventBus.send('cron.test.map.add', [scheduler_id: UUID.randomUUID().toString(), timer_id: 'ilio']) { result ->
        container.logger.info "map add result: ${result.body}"
        assertEquals('error', result.body.status)
        assertEquals('timer_id must be specified as a long', result.body.message)
        testComplete()
    }
}
def testAddSchedulerIdMissing() {

    vertx.eventBus.send('cron.test.map.add', [timer_id: new Date().getTime()]) { result ->
        container.logger.info "map add result: ${result.body}"
        assertEquals('error', result.body.status)
        assertEquals('scheduler_id must be specified as a string', result.body.message)
        testComplete()
    }
}
def testAddSchedulerIdBadType() {

    vertx.eventBus.send('cron.test.map.add', [scheduler_id: 666, timer_id: new Date().getTime()]) { result ->
        container.logger.info "map add result: ${result.body}"
        assertEquals('error', result.body.status)
        assertEquals('scheduler_id must be specified as a string', result.body.message)
        testComplete()
    }
}
def testRemoveSchedulerIdMissing() {

    vertx.eventBus.send('cron.test.map.remove', null) { result ->
        container.logger.info "map remove result: ${result.body}"
        assertEquals('error', result.body.status)
        assertEquals('message must be the scheduler_id as a string', result.body.message)
        testComplete()
    }
}
def testRemoveSchedulerIdBadType() {

    vertx.eventBus.send('cron.test.map.remove', 777) { result ->
        container.logger.info "map remove result: ${result.body}"
        assertEquals('error', result.body.status)
        assertEquals('message must be the scheduler_id as a string', result.body.message)
        testComplete()
    }
}
def testRemoveSchedulerIdNotExisting() {

    vertx.eventBus.send('cron.test.map.remove', UUID.randomUUID().toString()) { result ->
        container.logger.info "map remove result: ${result.body}"
        assertEquals('ok', result.body.status)
        testComplete()
    }
}
def testResultAddressBadType() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'none',
            message: 'manu',
            result_address: 456
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('result_address must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testResultAddress() {

    def scheduled_address = "address_${new Date().format('yyyyMMddhhmmssSSS')}".toString()
    def result_address = "address_${new Date().format('yyyyMMddhhmmssSSS')}".toString()

    vertx.eventBus.registerHandler(scheduled_address) { message ->
        container.logger.info "Handler message: ${message.body}"
        assertEquals('goose', message.body)
        message.reply('nene')
    }
    vertx.eventBus.registerHandler(result_address) { message ->
        container.logger.info "Result message: ${message.body}"
        assertEquals('nene', message.body)
        testComplete()
    }

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: scheduled_address,
            message: 'goose',
            result_address: result_address
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('ok', result.body.status)
        assertNotNull(result.body.scheduler_id)
        assertNotNull(result.body.next_run_time)
    }
}

VertxTests.initialize(this)

def config = [
        address_base: 'cron.test'
]

container.deployModule(System.getProperty("vertx.modulename"), config) { result ->

    if (!result.succeeded) {
       container.logger.error "Deployment failed with: ${result.cause()}"
    }

    assertTrue(result.succeeded)
    assertNotNull("deploymentID should not be null", result.result())

    VertxTests.startTests(this)
}




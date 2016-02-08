package com.diabolicallabs.vertx.cron;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.RxHelper;
import rx.Scheduler;

import java.util.Arrays;
import java.util.TimeZone;

public class CronEventSchedulerVertical extends AbstractVerticle {

  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    EventBus eb = vertx.eventBus();

    String addressBase = config().getString("address_base", "cron");

    String create_address = addressBase + ".schedule";

    eb.consumer(create_address, handler -> {

      if (!(handler.body() instanceof JsonObject)) throw new IllegalArgumentException("Message must be a JSON object");

      JsonObject message = (JsonObject) handler.body();

      try {
        if (!message.containsKey("cron_expression"))
          throw new IllegalArgumentException("Message must contain cron_expression");

        if (!message.containsKey("address"))
          throw new IllegalArgumentException("Message must contain the address to schedule");

        if (message.containsKey("timezone_name")) {
          Boolean noneMatch = Arrays.stream(TimeZone.getAvailableIDs())
            .noneMatch(available -> available.equals(message.getString("timezone_name")));
          if (noneMatch) throw new IllegalArgumentException("timezone_name " + message.getString("timezone_name") + " is invalid");
        }

        if (message.containsKey("action")) {
          String action = message.getString("action");
          if (!(action.equals("send") || action.equals("publish"))) {
            throw new IllegalArgumentException("action must be 'send' or 'publish'");
          }
        }
      } catch (IllegalArgumentException iae) {
        handler.fail(-1, iae.getMessage());
        return;
      }

      String cronExpression = message.getString("cron_expression");
      String timezoneName = message.getString("timezone_name");
      String scheduledAddress = message.getString("address");
      Object scheduledMessage = message.getString("message");
      String action = message.getString("action", "send");
      String resultAddress = message.getString("result_address");

      Scheduler scheduler = RxHelper.scheduler(vertx);
      CronObservable.cronspec(scheduler, cronExpression, timezoneName)
        .subscribe(
          timestamped -> {
            if (action.equals("send")) {
              eb.send(scheduledAddress, scheduledMessage, scheduledAddressHandler -> {
                if (resultAddress != null) {
                  if (scheduledAddressHandler.succeeded()) {
                    eb.send(resultAddress, scheduledAddressHandler.result().body());
                  } else {
                    if (scheduledAddressHandler.failed()) {
                      logger.error("Message to " + resultAddress + " failed.", scheduledAddressHandler.cause());
                    }
                  }
                }
              });
            } else {
              eb.publish(scheduledAddress, scheduledMessage);
            }
          },
          fault -> {
            logger.error("Unable to process cronspec " + cronExpression + " for address " + scheduledAddress, fault);
            handler.fail(-1, fault.getMessage());
          }
        );

    });

    startFuture.complete();
  }

}

package com.diabolicallabs.test.vertx.cron;

import com.diabolicallabs.vertx.cron.CronEventSchedulerVertical;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(io.vertx.ext.unit.junit.VertxUnitRunner.class)
public class CronEventBusAlternateBaseTest {

  private static final String BASE_ADDRESS = "alternate.schedule";

  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  @Before
  public void before(TestContext context) {

    JsonObject config = new JsonObject().put("address_base", "alternate");
    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    rule.vertx().deployVerticle(CronEventSchedulerVertical.class.getName(), options, context.asyncAssertSuccess(id -> {
      System.out.println("CronEventSchedulerVertical deployment id: " + id);
    }));
  }

  @Test
  public void testSend(TestContext context) {

    Async async = context.async();

    String address = UUID.randomUUID().toString();
    JsonObject event = event().put("address", address);
    AtomicBoolean gotit = new AtomicBoolean(false);

    rule.vertx().eventBus().consumer(address, handler -> {
      gotit.set(true);
    });

    rule.vertx().eventBus().send(BASE_ADDRESS, event, handler -> {
      if (handler.failed()) context.fail(handler.cause());
    });

    rule.vertx().setTimer(1000 * 2, timerHandler -> {
      context.assertTrue(gotit.get());
      async.complete();
    });
  }

  private JsonObject event() {

    JsonObject event = new JsonObject()
      .put("cron_expression", "*/1 * * * * ?")
      .put("address", "scheduled.address")
      .put("message", "squid")
      .put("action", "send")
      .put("result_address", "result.address");

    return event;
  }
}

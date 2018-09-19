package com.diabolicallabs.test.vertx.cron;

import com.diabolicallabs.vertx.cron.CronObservable;
import io.reactivex.Scheduler;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(io.vertx.ext.unit.junit.VertxUnitRunner.class)
public class CronRxTest {

  Scheduler scheduler;

  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  @Before
  public void before(TestContext context) {

    scheduler = RxHelper.scheduler(new Vertx(rule.vertx()));

  }

  @Test
  public void testNoTimezone(TestContext context) {

    Async async = context.async();

    AtomicInteger count = new AtomicInteger(0);

    CronObservable.cronspec(scheduler, "*/1 * * * * ?")
      .doOnNext(System.out::println)
      .take(3)
      .subscribe(
        timestamped -> {
          count.incrementAndGet();
        },
        context::fail,
        () -> {
          context.assertEquals(3, count.get());
          async.complete();
        }
      );
  }

  @Test
  public void testWithTimezone(TestContext context) {

    Async async = context.async();

    Calendar calendar = new GregorianCalendar();

    calendar.setTimeZone(TimeZone.getTimeZone("Pacific/Midway"));
    calendar.add(Calendar.SECOND, 2);

    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);
    int second = calendar.get(Calendar.SECOND);

    String cronspec = second + " " + minute + " " + hour + " * * ?";

    CronObservable.cronspec(scheduler, cronspec, "Pacific/Midway")
      .doOnNext(System.out::println)
      .take(1)
      .subscribe(
        context::assertNotNull,
        context::fail,
        async::complete
      );
  }


  @Test
  public void testBadCronspec(TestContext context) {

    Async async = context.async();

    CronObservable.cronspec(scheduler, "*/2 GOAT * ?")
      .doOnNext(System.out::println)
      .take(3)
      .subscribe(
        context::assertNull,
        fault -> {
          context.assertEquals("Invalid cronspec */2 GOAT * ?", fault.getMessage());
          async.complete();
        },
        async::complete
      );
  }


  @Test
  public void testBadTimeZone(TestContext context) {

    Async async = context.async();

    try {
      CronObservable.cronspec(scheduler, "*/2 * * * * ?", "USSR/Honolulu");
      context.fail("Bad timezone not caught");
    } catch (IllegalArgumentException iae) {
      context.assertEquals("timeZoneName USSR/Honolulu is invalid", iae.getMessage());
      async.complete();
    }
  }

}

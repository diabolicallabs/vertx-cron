package com.diabolicallabs.vertx.cron;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.quartz.CronExpression;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Timestamped;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CronObservable {

  private CronObservable() {
  }

  public static Observable<Timestamped<Long>> cronspec(Scheduler scheduler, String cronspec) {

    return CronObservable.cronspec(scheduler, cronspec, null);
  }

  public static Observable<Timestamped<Long>> cronspec(Scheduler scheduler, String cronspec, String timeZoneName) {

    if (timeZoneName != null) {
      Boolean noneMatch = Arrays.stream(TimeZone.getAvailableIDs()).noneMatch(available -> available.equals(timeZoneName));
      if (noneMatch) throw new IllegalArgumentException("timeZoneName " + timeZoneName + " is invalid");
    }

    return Observable.just(cronspec)
      .flatMap(_cronspec -> {
        CronExpression cron;
        try {
          cron = new CronExpression(_cronspec);
          if (timeZoneName != null) {
            cron.setTimeZone(TimeZone.getTimeZone(timeZoneName));
          }
        } catch (ParseException e) {
          throw new IllegalArgumentException("Invalid cronspec " + _cronspec, e);
        }

        return Observable.just(cron)
          .map(cronExpression -> {
            return cronExpression.getNextValidTimeAfter(new Date());
          })
          .map(nextRunDate -> {
            return nextRunDate.getTime() - new Date().getTime();
          })
          .flatMap(delay -> {
            return Observable.timer(delay, TimeUnit.MILLISECONDS, scheduler);
          })
          .timestamp()
          .repeat();
      });
  }

}

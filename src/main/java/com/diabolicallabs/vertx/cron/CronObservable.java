package com.diabolicallabs.vertx.cron;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Timed;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CronObservable {

  private CronObservable() {
  }

  public static Observable<Timed<Long>> cronspec(Scheduler scheduler, String cronspec) {

    return CronObservable.cronspec(scheduler, cronspec, null);
  }

  public static Observable<Timed<Long>> cronspec(Scheduler scheduler, String cronspec, String timeZoneName) {

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
          .map(cronExpression -> cronExpression.getNextValidTimeAfter(new Date(new Date().getTime() + 500)))
          .map(nextRunDate -> nextRunDate.getTime() - new Date().getTime())
          .flatMap(delay -> Observable.timer(delay, TimeUnit.MILLISECONDS, scheduler))
          .timestamp()
          .repeat();
      });
  }

}

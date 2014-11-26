package djgcv.ssjp.util;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ActivityTimeout implements Runnable {
  private Future<?> taskFuture;
  private long timeout;

  protected ActivityTimeout(long delay, TimeUnit unit, boolean started) {
    if (delay <= 0) {
      throw new IllegalArgumentException("Delay must be greater than zero: "
          + delay + " " + unit);
    }
    setDefaultTimeout(delay, unit);
    if (started) {
      restart();
    }
  }

  public abstract ScheduledExecutorService getExecutor();

  public synchronized long getTimeout(TimeUnit unit) {
    return unit.convert(timeout, TimeUnit.NANOSECONDS);
  }

  public synchronized void setTimeout(long delay, TimeUnit unit) {
    setDefaultTimeout(delay, unit);
    restart();
  }

  public synchronized void setDefaultTimeout(long delay, TimeUnit unit) {
    timeout = unit.toNanos(delay);
  }

  public synchronized void restart() {
    if (taskFuture != null) {
      taskFuture.cancel(false);
    }
    taskFuture = getExecutor().schedule(this, timeout, TimeUnit.NANOSECONDS);
  }

  public synchronized void stop() {
    if (taskFuture != null) {
      taskFuture.cancel(false);
      taskFuture = null;
    }
  }
}

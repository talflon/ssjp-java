package djgcv.ssjp.util;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ActivityTimeout implements RestartableTimeout, Runnable {
  private Future<?> taskFuture;
  private long timeout;

  protected ActivityTimeout(long delay, TimeUnit unit) {
    if (delay <= 0) {
      throw new IllegalArgumentException("Delay must be greater than zero: "
          + delay + " " + unit);
    }
    setDefault(delay, unit);
  }

  public abstract ScheduledExecutorService getExecutor();

  public synchronized long getDefault(TimeUnit unit) {
    return unit.convert(timeout, TimeUnit.NANOSECONDS);
  }

  @Override
  public synchronized void set(long delay, TimeUnit unit) {
    setDefault(delay, unit);
    restart();
  }

  public synchronized void setDefault(long delay, TimeUnit unit) {
    timeout = unit.toNanos(delay);
  }

  @Override
  public synchronized void restart() {
    if (taskFuture != null) {
      taskFuture.cancel(false);
    }
    taskFuture = getExecutor().schedule(this, timeout, TimeUnit.NANOSECONDS);
  }

  @Override
  public synchronized void stop() {
    if (taskFuture != null) {
      taskFuture.cancel(false);
      taskFuture = null;
    }
  }
}

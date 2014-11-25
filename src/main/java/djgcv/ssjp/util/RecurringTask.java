package djgcv.ssjp.util;

import java.util.concurrent.Executor;

public abstract class RecurringTask implements Runnable {
  private boolean running = false;

  protected abstract boolean shouldRun();

  protected abstract void doTask();

  public abstract Executor getExecutor();

  protected synchronized void finishedRunning() {
    running = false;
    trigger();
  }

  public synchronized void trigger() {
    if (shouldRun()) {
      if (!running) {
        getExecutor().execute(this);
      }
      running = true;
    }
  }

  @Override
  public void run() {
    doTask();
    finishedRunning();
  }
}

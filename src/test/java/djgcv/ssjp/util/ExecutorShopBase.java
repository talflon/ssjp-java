package djgcv.ssjp.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class ExecutorShopBase extends SafeCloseableTestBase
    implements ThreadFactory, UncaughtExceptionHandler {
  private ExecutorShop executorShop;

  public ExecutorShop getExecutorShop() {
    return executorShop;
  }

  protected void setExecutorShop(ExecutorShop executorShop) {
    this.executorShop = executorShop;
  }

  protected void setExecutorShop(int numProcessors) {
    setExecutorShop(ExecutorShops.create(numProcessors, this));
  }

  protected void setExecutorShop() {
    setExecutorShop(2);
  }

  @Override
  protected void finalTearDown() throws Exception {
    ExecutorShop executorShop = getExecutorShop();
    try {
      super.finalTearDown();
      executorShop.getScheduler().shutdown();
      executorShop.getExecutor().shutdown();
      executorShop.getBlockingExecutor().shutdown();
      if (!executorShop.getScheduler().awaitTermination(10, TimeUnit.SECONDS)) {
        throw new TimeoutException("Scheduler's tasks did not finish");
      }
      if (!executorShop.getExecutor().awaitTermination(10, TimeUnit.SECONDS)) {
        throw new TimeoutException("Executor's tasks did not finish");
      }
      if (!executorShop.getBlockingExecutor().awaitTermination(
          10, TimeUnit.SECONDS)) {
        throw new TimeoutException("Blocking executor's tasks did not finish");
      }
    } finally {
      executorShop.getScheduler().shutdownNow();
      executorShop.getExecutor().shutdownNow();
      executorShop.getBlockingExecutor().shutdownNow();
    }
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    close(e);
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(r);
    thread.setUncaughtExceptionHandler(this);
    return thread;
  }
}

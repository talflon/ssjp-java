package djgcv.ssjp.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class ExecutorTestBase<E extends ExecutorService>
    extends SafeCloseableTestBase
    implements ThreadFactory, UncaughtExceptionHandler {
  private E executor;

  public E getExecutor() {
    return executor;
  }

  protected void setExecutor(E executor) {
    this.executor = executor;
  }

  @Override
  protected void finalTearDown() throws Exception {
    ExecutorService executor = getExecutor();
    try {
      super.finalTearDown();
      executor.shutdown();
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        throw new TimeoutException("Executor's tasks did not finish");
      }
    } finally {
      executor.shutdownNow();
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

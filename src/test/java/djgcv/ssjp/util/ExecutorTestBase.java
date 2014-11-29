package djgcv.ssjp.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;

public abstract class ExecutorTestBase<E extends ExecutorService> extends SafeCloseableTestBase {
  private final E executor;

  public ExecutorTestBase(E executor) {
    this.executor = executor;
  }

  public E getExecutor() {
    return executor;
  }

  @After
  @Override
  public void finalTearDown() throws Exception {
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
}

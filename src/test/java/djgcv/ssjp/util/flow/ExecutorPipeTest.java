package djgcv.ssjp.util.flow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;

public class ExecutorPipeTest extends BasePipeTest {
  ExecutorService executor;

  @Before
  public void setUp() throws Exception {
    executor = Executors.newCachedThreadPool();
  }

  @After
  public void tearDown() throws Exception {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  @Override
  protected <T> Pipe<T> createPipe() {
    return new ExecutorPipe<T>(executor);
  }
}

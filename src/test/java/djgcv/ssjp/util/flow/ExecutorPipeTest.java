package djgcv.ssjp.util.flow;

import org.junit.Before;

public class ExecutorPipeTest extends BasePipeTest {
  @Before
  public void setUp() {
    setExecutorShop();
  }

  @Override
  protected <T> Pipe<T> createPipe() {
    return new ExecutorPipe<T>(getExecutorShop().getBlockingExecutor());
  }
}

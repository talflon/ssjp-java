package djgcv.ssjp.util.flow;

import org.junit.Before;

public class ConcurrentPipeTest extends BasePipeTest {
  @Before
  public void setUp() {
    setExecutorShop();
  }

  @Override
  protected <T> Pipe<T> createPipe() {
    return new ConcurrentPipe<T>(getExecutorShop().getBlockingExecutor());
  }
}

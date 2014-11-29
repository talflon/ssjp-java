package djgcv.ssjp.util.flow;

import java.util.concurrent.Executors;

import org.junit.Before;

import com.google.common.util.concurrent.MoreExecutors;

public class ConcurrentPipeTest extends BasePipeTest {
  @Before
  public void setUp() {
    setExecutor(MoreExecutors.listeningDecorator(
        Executors.newCachedThreadPool(this)));
  }

  @Override
  protected <T> Pipe<T> createPipe() {
    return new ConcurrentPipe<T>(getExecutor());
  }
}

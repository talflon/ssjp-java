package djgcv.ssjp.util.flow;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;

import djgcv.ssjp.util.ExecutorTestBase;

public abstract class BasePipeTest
    extends ExecutorTestBase<ListeningExecutorService> {
  protected abstract <T> Pipe<T> createPipe();

  @Test
  public void testReceive() throws Exception {
    for (int numReceivers = 1; numReceivers <= 5; numReceivers++) {
      String value = "TEST " + numReceivers;
      Pipe<String> pipe = createPipe();
      List<Future<String>> futures = Lists.newArrayList();
      for (int i = 0; i < numReceivers; i++) {
        FutureHandler<String> future = new FutureHandler<String>();
        futures.add(future);
        pipe.getOutput().appendReceiver(future);
      }
      pipe.getInput().receive(value);
      for (Future<String> future : futures) {
        assertEquals(value, future.get(1, TimeUnit.SECONDS));
      }
    }
  }
}

package djgcv.ssjp.util.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import djgcv.ssjp.util.ExecutorTestBase;
import djgcv.ssjp.util.MockScheduledExecutor;

public class StreamTimeoutTest
    extends ExecutorTestBase<ListeningExecutorService> {
  MockScheduledExecutor scheduler;
  MockStream stream;
  TimeoutCounter counter;

  @Before
  public void setUp() throws Exception {
    setExecutor(MoreExecutors.listeningDecorator(
        Executors.newCachedThreadPool(this)));
    scheduler = new MockScheduledExecutor(getExecutor());
    stream = new MockStream();
    counter = new TimeoutCounter(stream);
  }

  static class MockStream implements ActivityCallbackStream {
    volatile Runnable callback;

    @Override
    public Runnable getCallback() {
      return callback;
    }

    @Override
    public void setCallback(Runnable callback) {
      this.callback = callback;
    }
  }

  class TimeoutCounter extends StreamTimeout<MockStream> {
    final AtomicInteger count = new AtomicInteger(0);

    protected TimeoutCounter(MockStream stream) {
      super(stream);
    }

    @Override
    protected ScheduledExecutorService getExecutor() {
      return scheduler;
    }

    @Override
    protected void onTimeout() {
      count.incrementAndGet();
    }
  }

  void runImmediateEvents() throws Exception {
    Futures.allAsList(scheduler.startEvents()).get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testCounterNotLeakedInConstructor() {
    assertNull(stream.getCallback());
  }

  @Test
  public void testBeforeTimeout() throws Exception {
    counter.set(120, TimeUnit.SECONDS);
    scheduler.shiftTime(119, TimeUnit.SECONDS);
    runImmediateEvents();
    assertEquals(0, counter.count.get());
  }

  @Test
  public void testAfterTimeout() throws Exception {
    counter.set(60, TimeUnit.SECONDS);
    scheduler.shiftTime(60, TimeUnit.SECONDS);
    runImmediateEvents();
    assertEquals(1, counter.count.get());
  }

  void delayTimeout(int times, long after, TimeUnit unit) throws Exception {
    for (int i = 0; i < times; i++) {
      scheduler.shiftTime(after, unit);
      runImmediateEvents();
      stream.getCallback().run();
    }
  }

  @Test
  public void testBeforeDelayedTimeout() throws Exception {
    for (int numDelays = 1; numDelays < 10; numDelays++) {
      int count = counter.count.get();
      counter.set(10, TimeUnit.SECONDS);
      runImmediateEvents();
      delayTimeout(numDelays, 9, TimeUnit.SECONDS);
      scheduler.shiftTime(9, TimeUnit.SECONDS);
      assertEquals(count, counter.count.get());
    }
  }

  @Test
  public void testAfterDelayedTimeout() throws Exception {
    for (int numDelays = 1; numDelays < 10; numDelays++) {
      int count = counter.count.get();
      counter.set(10, TimeUnit.SECONDS);
      runImmediateEvents();
      delayTimeout(numDelays, 9, TimeUnit.SECONDS);
      scheduler.shiftTime(10, TimeUnit.SECONDS);
      runImmediateEvents();
      assertEquals(count + 1, counter.count.get());
    }
  }

  @Test
  public void testAfterStopped() throws Exception {
    counter.set(5, TimeUnit.SECONDS);
    counter.stop();
    assertEquals(Long.MIN_VALUE, scheduler.startNextEvent());
  }

  @Test
  public void testTimeoutAfterRestart() throws Exception {
    counter.set(4, TimeUnit.SECONDS);
    scheduler.shiftTime(1, TimeUnit.SECONDS);
    runImmediateEvents();
    counter.stop();
    scheduler.shiftTime(1, TimeUnit.SECONDS);
    runImmediateEvents();
    counter.set(12, TimeUnit.SECONDS);
    scheduler.shiftTime(11, TimeUnit.SECONDS);
    runImmediateEvents();
    assertEquals(0, counter.count.get());
    scheduler.shiftTime(1, TimeUnit.SECONDS);
    runImmediateEvents();
    assertEquals(1, counter.count.get());
  }
}

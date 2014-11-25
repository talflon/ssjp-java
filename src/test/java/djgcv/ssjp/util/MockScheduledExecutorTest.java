package djgcv.ssjp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Runnables;

public class MockScheduledExecutorTest {
  MockScheduledExecutor scheduler;

  @Before
  public void setUp() throws Exception {
    scheduler = new MockScheduledExecutor(
        MoreExecutors.listeningDecorator(Executors.newCachedThreadPool()));
  }

  @After
  public void tearDown() throws Exception {
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }

  @Test
  public void testScheduleCallable() throws Exception {
    final String value = "hi";
    Future<String> future = scheduler.schedule(Callables.returning(value), 9,
        TimeUnit.SECONDS);
    scheduler.startNextEvent();
    assertEquals(value, future.get(1, TimeUnit.SECONDS));
  }

  @Test
  public void testScheduleRunnable() throws Exception {
    Future<?> future = scheduler.schedule(Runnables.doNothing(), 5,
        TimeUnit.SECONDS);
    assertEquals(0, scheduler.startEvents());
    assertFalse(future.isDone());
    scheduler.shiftTime(4, TimeUnit.SECONDS);
    assertEquals(0, scheduler.startEvents());
    assertFalse(future.isDone());
    scheduler.shiftTime(1, TimeUnit.SECONDS);
    assertEquals(1, scheduler.startEvents());
    future.get(1, TimeUnit.SECONDS);
  }
}

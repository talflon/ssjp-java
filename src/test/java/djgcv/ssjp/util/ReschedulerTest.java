package djgcv.ssjp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Runnables;

public class ReschedulerTest {
  MockScheduledExecutor scheduler;
  Rescheduler rescheduler;

  @Before
  public void setUp() throws Exception {
    scheduler = new MockScheduledExecutor(
        MoreExecutors.listeningDecorator(Executors.newCachedThreadPool()));
    rescheduler = new Rescheduler(scheduler);
  }

  @After
  public void tearDown() throws Exception {
    if (rescheduler != null) {
      rescheduler.shutdownNow();
    }
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }

  @Test
  public void testReschedule() throws Exception {
    ReschedulableFuture<?> future = rescheduler.schedule(Runnables.doNothing(),
        5, TimeUnit.SECONDS);
    scheduler.shiftTime(4, TimeUnit.SECONDS);
    scheduler.startEvents();
    assertTrue(future.reschedule(10, TimeUnit.SECONDS));
    scheduler.shiftTime(7, TimeUnit.SECONDS);
    assertEquals(0, scheduler.startEvents());
    assertFalse(future.isDone());
    scheduler.shiftTime(3, TimeUnit.SECONDS);
    assertEquals(1, scheduler.startEvents());
    future.get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testRescheduleAfterDone() throws Exception {
    ReschedulableFuture<?> future = rescheduler.schedule(Runnables.doNothing(),
        2, TimeUnit.SECONDS);
    scheduler.shiftTime(3, TimeUnit.SECONDS);
    scheduler.startEvents();
    future.get(1, TimeUnit.SECONDS);
    assertFalse(future.reschedule(1, TimeUnit.SECONDS));
  }
}

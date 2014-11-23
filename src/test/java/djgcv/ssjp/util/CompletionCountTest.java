package djgcv.ssjp.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

public class CompletionCountTest {
  static void checkRemoveTask(CompletionCount count, int numTasks) {
    for (int i = 0; i < numTasks; i++) {
      assertFalse(count.isDone());
      count.removeTask();
    }
    assertTrue(count.isDone());
  }

  @Test
  public void testRemoveTask() {
    for (int numTasks = 1; numTasks < 10; numTasks++) {
      CompletionCount count = new CompletionCount(numTasks);
      checkRemoveTask(count, numTasks);
    }
  }

  @Test
  public void testAddTask() {
    for (int numTasks = 1; numTasks < 10; numTasks++) {
      CompletionCount count = new CompletionCount(1);
      for (int i = 1; i < numTasks; i++) {
        count.addTask();
      }
      checkRemoveTask(count, numTasks);
    }
  }

  @Test
  public void testAddChild() throws Exception {
    CompletionCount count = new CompletionCount(1);
    SettableFuture<?> future = SettableFuture.create();
    count.addChild(future);
    count.removeTask();
    assertFalse(count.isDone());
    future.set(null);
    count.get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testAddChildAlreadyDone() throws Exception {
    CompletionCount count = new CompletionCount(1);
    count.addChild(Futures.immediateFuture(null));
    count.removeTask();
    count.get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testConstructBadCount() {
    for (int numTasks = 0; numTasks > -5; numTasks--) {
      try {
        new CompletionCount(numTasks);
        fail("Constructed with " + numTasks);
      } catch (IllegalArgumentException e) {
        /* pass */
      }
    }
  }

  @Test
  public void testCountDownTooFar() {
    for (int numTasks = 1; numTasks < 5; numTasks++) {
      CompletionCount count = new CompletionCount(numTasks);
      for (int i = 0; i < numTasks; i++) {
        count.removeTask();
      }
      try {
        count.removeTask();
        fail("Counted down past zero");
      } catch (IllegalStateException e) {
        /* pass */
      }
    }
  }

  @Test
  public void testCountUpAfterZero() {
    for (int numTasks = 1; numTasks < 5; numTasks++) {
      CompletionCount count = new CompletionCount(numTasks);
      for (int i = 0; i < numTasks; i++) {
        count.removeTask();
      }
      try {
        count.addTask();
        fail("Counted up after zero");
      } catch (IllegalStateException e) {
        /* pass */
      }
    }
  }
}

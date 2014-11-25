package djgcv.ssjp.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

public class RecurringTaskTest {
  @Test
  public void testShouldntRun() {
    RecurringTask task = new RecurringTask() {
      @Override
      protected boolean shouldRun() {
        return false;
      }

      @Override
      public Executor getExecutor() {
        return MoreExecutors.sameThreadExecutor();
      }

      @Override
      protected void doTask() {
        fail("Wasn't supposed to run");
      }
    };
    task.trigger();
  }

  @Test
  public void testRunsUntilFinished() throws Exception {
    final SettableFuture<?> finished = SettableFuture.create();
    final List<Integer> list = Collections.synchronizedList(
        Lists.newArrayList(1, 2, 3, 4, 5));
    RecurringTask task = new RecurringTask() {
      @Override
      protected boolean shouldRun() {
        return !list.isEmpty();
      }

      @Override
      public Executor getExecutor() {
        return MoreExecutors.sameThreadExecutor();
      }

      @Override
      protected void doTask() {
        list.remove(0);
        if (list.isEmpty()) {
          finished.set(null);
        }
      }
    };
    task.trigger();
    finished.get(1, TimeUnit.SECONDS);
    assertTrue(list.isEmpty());
  }
}

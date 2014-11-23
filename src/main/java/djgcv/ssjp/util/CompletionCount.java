package djgcv.ssjp.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class CompletionCount extends AbstractFuture<Object> {
  private final AtomicInteger numTasks;

  public CompletionCount(int numTasks) {
    if (numTasks < 1) {
      throw new IllegalArgumentException("CompletionCount called with "
          + numTasks + " tasks; must be at least one");
    }
    this.numTasks = new AtomicInteger(numTasks);
  }

  public void addTask() {
    if (numTasks.incrementAndGet() == 1) {
      numTasks.decrementAndGet();
      throw new IllegalStateException("CompletionCount added to after becoming zero");
    }
  }

  public void removeTask() {
    int numTasks = this.numTasks.decrementAndGet();
    if (numTasks == 0) {
      set(null);
    } else if (numTasks < 0) {
      this.numTasks.incrementAndGet();
      throw new IllegalStateException("CompletionCount removed from after becoming zero");
    }
  }

  public void addChild(ListenableFuture<?> child) {
    addTask();
    child.addListener(new Runnable() {
      @Override
      public void run() {
        removeTask();
      }
    }, MoreExecutors.sameThreadExecutor());
  }
}

package djgcv.ssjp.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public final class ExecutorShops {
  public static ExecutorShop create(int numProcessors,
      ThreadFactory threadFactory) {
    if (numProcessors <= 0) {
      throw new IllegalArgumentException("numProcessors must be > 0");
    }
    final ListeningScheduledExecutorService cpuExec =
        MoreExecutors.listeningDecorator(
            Executors.newScheduledThreadPool(numProcessors, threadFactory));
    final ListeningExecutorService blockingExec =
        MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool(threadFactory));
    return new ExecutorShop() {
      @Override
      public ListeningScheduledExecutorService getScheduler() {
        return cpuExec;
      }

      @Override
      public ListeningExecutorService getExecutor() {
        return cpuExec;
      }

      @Override
      public ListeningExecutorService getBlockingExecutor() {
        return blockingExec;
      }
    };
  }

  public static ExecutorShop create(int numProcessors) {
    return create(numProcessors, Executors.defaultThreadFactory());
  }

  public static ExecutorShop create(ThreadFactory threadFactory) {
    return create(Runtime.getRuntime().availableProcessors(), threadFactory);
  }

  public static ExecutorShop create() {
    return create(Executors.defaultThreadFactory());
  }

  private ExecutorShops() { }
}

package djgcv.ssjp.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

public interface ExecutorShop {
  ListeningExecutorService getBlockingExecutor();

  ListeningExecutorService getExecutor();

  ListeningScheduledExecutorService getScheduler();
}

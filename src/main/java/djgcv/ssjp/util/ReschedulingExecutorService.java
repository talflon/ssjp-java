package djgcv.ssjp.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

public interface ReschedulingExecutorService extends
    ListeningScheduledExecutorService {
  <V> ReschedulableFuture<V> schedule(Callable<V> callable, long delay,
      TimeUnit unit);

  ReschedulableFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
}

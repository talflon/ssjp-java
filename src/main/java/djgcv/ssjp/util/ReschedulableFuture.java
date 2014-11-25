package djgcv.ssjp.util;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableScheduledFuture;

public interface ReschedulableFuture<V> extends ListenableScheduledFuture<V> {
  boolean reschedule(long delay, TimeUnit unit);
}

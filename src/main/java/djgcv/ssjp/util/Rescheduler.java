package djgcv.ssjp.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ExecutionList;
import com.google.common.util.concurrent.ForwardingFuture;
import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class Rescheduler extends ForwardingListeningExecutorService implements
    ReschedulingExecutorService {
  private final ListeningScheduledExecutorService delegateExecutor;

  public Rescheduler(ListeningScheduledExecutorService delegateExecutor) {
    this.delegateExecutor = delegateExecutor;
  }

  @Override
  protected ListeningScheduledExecutorService delegate() {
    return delegateExecutor;
  }

  @Override
  public ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable command,
      long initialDelay, long period, TimeUnit unit) {
    return delegate().scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  @Override
  public ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
      long initialDelay, long delay, TimeUnit unit) {
    return delegate().scheduleWithFixedDelay(command,
        initialDelay, delay, unit);
  }

  @Override
  public <V> ReschedulableFuture<V> schedule(final Callable<V> callable,
      long delay, TimeUnit unit) {
    return new RFuture<V>() {
      @Override
      protected ListenableScheduledFuture<V> createDelegateFuture(long delay,
          TimeUnit unit) {
        return Rescheduler.this.delegate().schedule(callable, delay, unit);
      }
    }.start(delay, unit);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public ReschedulableFuture<?> schedule(final Runnable command, long delay,
      TimeUnit unit) {
    return new RFuture() {
      @Override
      protected ListenableScheduledFuture<?> createDelegateFuture(long delay,
          TimeUnit unit) {
        return Rescheduler.this.delegate().schedule(command, delay, unit);
      }
    }.start(delay, unit);
  }

  protected abstract class RFuture<V> extends ForwardingFuture<V> implements
      ReschedulableFuture<V> {
    private final ExecutionList executionList = new ExecutionList();
    private ListenableScheduledFuture<V> delegateFuture;

    protected abstract ListenableScheduledFuture<V> createDelegateFuture(
        long delay, TimeUnit unit);

    RFuture<V> start(long delay, TimeUnit unit) {
      initDelegateFuture(delay, unit);
      return this;
    }

    private void initDelegateFuture(long delay, TimeUnit unit) {
      delegateFuture = createDelegateFuture(delay, unit);
      delegateFuture.addListener(new Runnable() {
        @Override
        public void run() {
          executionList.execute();
        }
      }, MoreExecutors.sameThreadExecutor());
    }

    @Override
    public synchronized boolean reschedule(long delay, TimeUnit unit) {
      boolean success = delegateFuture.cancel(false);
      if (success) {
        initDelegateFuture(delay, unit);
      }
      return success;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return delegate().getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
      return delegate().compareTo(o);
    }

    @Override
    protected synchronized ListenableScheduledFuture<V> delegate() {
      return delegateFuture;
    }

    @Override
    public void addListener(Runnable runnable, Executor exec) {
      executionList.add(runnable, exec);
    }
  }
}

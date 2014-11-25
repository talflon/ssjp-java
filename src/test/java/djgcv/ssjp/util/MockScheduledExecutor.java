package djgcv.ssjp.util;

import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Ticker;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ForwardingExecutorService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

class MockScheduledExecutor extends ForwardingExecutorService implements
    ListeningScheduledExecutorService {
  private final PriorityQueue<Task<?>> taskQueue = Queues.newPriorityQueue();
  private final ListeningExecutorService delegateExecutor;
  private long elapsedTime = 0;

  public MockScheduledExecutor(ListeningExecutorService delegateExecutor) {
    this.delegateExecutor = delegateExecutor;
  }

  private final Ticker ticker = new Ticker() {
    @Override
    public long read() {
      synchronized (taskQueue) {
        return elapsedTime;
      }
    }
  };

  public Ticker getTicker() {
    return ticker;
  }

  public void shiftTime(long amount, TimeUnit unit) {
    synchronized (taskQueue) {
      elapsedTime += unit.toNanos(amount);
    }
  }

  public boolean isEmpty() {
    synchronized (taskQueue) {
      return taskQueue.isEmpty();
    }
  }

  public int startEvents() {
    int count = 0;
    synchronized (taskQueue) {
      while (!taskQueue.isEmpty()
          && taskQueue.peek().getDelay(TimeUnit.NANOSECONDS) <= 0) {
        execute(taskQueue.remove());
        count++;
      }
    }
    return count;
  }

  public long startNextEvent(TimeUnit unit) {
    long remaining;
    Task<?> task;
    synchronized (taskQueue) {
      if (taskQueue.isEmpty()) {
        return 0;
      }
      task = taskQueue.remove();
      remaining = task.finishTime - getTicker().read();
      if (remaining > 0) {
        elapsedTime += remaining;
      } else {
        remaining = 0;
      }
    }
    execute(task);
    return remaining;
  }

  public long startNextEvent() {
    return startNextEvent(TimeUnit.NANOSECONDS);
  }

  private class Task<V> extends AbstractFuture<V> implements
      ListenableScheduledFuture<V>, Runnable {
    final Callable<V> callable;
    final long finishTime;

    Task(Callable<V> callable, long finishTime) {
      this.callable = callable;
      this.finishTime = getTicker().read() + finishTime;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit
          .convert(finishTime - getTicker().read(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
      return Long.compare(getDelay(TimeUnit.NANOSECONDS),
          o.getDelay(TimeUnit.NANOSECONDS));
    }

    @Override
    public void run() {
      try {
        set(callable.call());
      } catch (Exception e) {
        setException(e);
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      synchronized (taskQueue) {
        if (!super.cancel(mayInterruptIfRunning)) {
          return false;
        }
        return taskQueue.remove(this);
      }
    }
  }

  @Override
  public <T> ListenableFuture<T> submit(Callable<T> callable) {
    return delegate().submit(callable);
  }

  @Override
  public ListenableFuture<?> submit(Runnable command) {
    return delegate().submit(command);
  }

  @Override
  public <T> ListenableFuture<T> submit(Runnable command, T value) {
    return delegate().submit(command, value);
  }

  @Override
  public ListenableScheduledFuture<?> schedule(final Runnable command,
      long delay, TimeUnit unit) {
    return schedule(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        command.run();
        return null;
      }
    }, delay, unit);
  }

  @Override
  public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable,
      long delay, TimeUnit unit) {
    Task<V> task = new Task<V>(callable, unit.toNanos(delay));
    synchronized (taskQueue) {
      taskQueue.add(task);
    }
    return task;
  }

  @Override
  public ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable command,
      long initialDelay, long period, TimeUnit unit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
      long initialDelay, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  protected ListeningExecutorService delegate() {
    return delegateExecutor;
  }
}
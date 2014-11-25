package djgcv.ssjp.util.flow;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

import djgcv.ssjp.util.RecurringTask;

public class ConcurrentPipe<T> extends
    AbstractGenericPipe<T, Receiver<? super T>> implements Pipe<T> {
  private final Deque<T> queue = new ArrayDeque<T>();
  private final Executor executor;

  public ConcurrentPipe(Executor executor) {
    this.executor = executor;
  }

  public Executor getExecutor() {
    return executor;
  }

  protected boolean isQueueEmpty() {
    synchronized (queue) {
      return queue.isEmpty();
    }
  }

  protected void addQueue(T value) {
    synchronized (queue) {
      queue.addLast(value);
    }
    task.trigger();
  }

  protected T popQueue() {
    synchronized (queue) {
      return queue.removeFirst();
    }
  }

  @Override
  public Receiver<? super T> getInput() {
    return input;
  }

  private final Receiver<T> input = new Receiver<T>() {
    @Override
    public void receive(T value) {
      addQueue(value);
    }
  };

  private final RecurringTask task = new RecurringTask() {
    @Override
    public void doTask() {
      final T value;
      try {
        value = popQueue();
      } catch (NoSuchElementException e) {
        return;
      }
      for (Receiver<? super T> receiver : getOutput().getReceivers()) {
        receiver.receive(value);
      }
    }

    @Override
    protected boolean shouldRun() {
      return !isQueueEmpty();
    }

    @Override
    public Executor getExecutor() {
      return ConcurrentPipe.this.getExecutor();
    }
  };
}

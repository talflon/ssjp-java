package djgcv.ssjp.util.flow;

import java.util.concurrent.Executor;

public class ExecutorPipe<T> extends AbstractPipe<T> {
  private final Executor executor;

  public ExecutorPipe(Executor executor) {
    this.executor = executor;
  }

  public Executor getExecutor() {
    return executor;
  }

  @Override
  public Receiver<? super T> getInput() {
    return input;
  }

  private final Receiver<T> input = new Receiver<T>() {
    @Override
    public boolean receive(final T value) {
      Executor executor = getExecutor();
      boolean handled = false;
      for (final Receiver<? super T> receiver : getOutput().getReceivers()) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            receiver.receive(value);
          }
        });
        handled = true;
      }
      return handled;
    }
  };
}

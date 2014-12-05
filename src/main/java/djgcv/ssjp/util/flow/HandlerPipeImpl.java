package djgcv.ssjp.util.flow;

public class HandlerPipeImpl<T> extends AbstractPipe<T> {
  @Override
  public Receiver<? super T> getInput() {
    return input;
  }

  private final Receiver<T> input = new Receiver<T>() {
    @Override
    public boolean receive(T value) {
      return Handlers.tryHandle(getOutput().getReceivers(), value);
    }
  };
}

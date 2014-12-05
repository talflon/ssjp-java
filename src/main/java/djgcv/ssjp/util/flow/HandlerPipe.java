package djgcv.ssjp.util.flow;

public class HandlerPipe<T> extends AbstractPipe<T> {
  @Override
  public Receiver<? super T> getInput() {
    return input;
  }

  private final Receiver<T> input = new Receiver<T>() {
    @Override
    public boolean receive(T value) {
      return propagateFirst(getOutput().getReceivers(), value);
    }
  };

  public static <T> boolean propagateFirst(
      Iterable<Receiver<? super T>> handlers, T value) {
    for (Receiver<? super T> handler : handlers) {
      if (handler.receive(value)) {
        return true;
      }
    }
    return false;
  }
}

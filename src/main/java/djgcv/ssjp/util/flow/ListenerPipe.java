package djgcv.ssjp.util.flow;

public class ListenerPipe<T> extends AbstractPipe<T> {
  private final Receiver<T> input = new Receiver<T>() {
    @Override
    public boolean receive(T value) {
      return propagateAll(getOutput().getReceivers(), value);
    }
  };

  @Override
  public Receiver<? super T> getInput() {
    return input;
  }

  public static <T> boolean propagateAll(
      Iterable<Receiver<? super T>> listeners, T value) {
    boolean handled = false;
    for (Receiver<? super T> listener : listeners) {
      handled |= listener.receive(value);
    }
    return handled;
  }
}

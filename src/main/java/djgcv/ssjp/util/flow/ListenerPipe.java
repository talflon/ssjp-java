package djgcv.ssjp.util.flow;

public class ListenerPipe<T> extends AbstractPipe<T> {
  private final Receiver<T> input = new Receiver<T>() {
    @Override
    public boolean receive(T value) {
      boolean handled = false;
      for (Receiver<? super T> receiver : getOutput().getReceivers()) {
        handled |= receiver.receive(value);
      }
      return handled;
    }
  };

  @Override
  public Receiver<? super T> getInput() {
    return input;
  }
}

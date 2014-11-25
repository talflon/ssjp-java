package djgcv.ssjp.util.flow;

public class PipeImpl<T> extends AbstractGenericPipe<T, Receiver<? super T>> implements Pipe<T> {
  private final Receiver<T> input = new Receiver<T>() {
    @Override
    public void receive(T value) {
      for (Receiver<? super T> receiver : getOutput().getReceivers()) {
        receiver.receive(value);
      }
    }
  };

  @Override
  public Receiver<? super T> getInput() {
    return input;
  }
}

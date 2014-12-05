package djgcv.ssjp.util.flow;

public abstract class AbstractPipe<T> implements Pipe<T> {
  private final ReceiverList<T> output;

  protected AbstractPipe(ReceiverList<T> output) {
    this.output = output;
  }

  protected AbstractPipe() {
    this(new ReceiverListImpl<T>());
  }

  @Override
  public ReceiverList<T> getOutput() {
    return output;
  }
}

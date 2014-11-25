package djgcv.ssjp.util.flow;

public abstract class AbstractGenericPipe<T, R extends Receiver<? super T>> implements GenericPipe<T, R> {
  private final ReceiverList<R> output;

  protected AbstractGenericPipe(ReceiverList<R> output) {
    this.output = output;
  }

  protected AbstractGenericPipe() {
    this(new ReceiverListImpl<T, R>());
  }

  @Override
  public ReceiverList<R> getOutput() {
    return output;
  }
}

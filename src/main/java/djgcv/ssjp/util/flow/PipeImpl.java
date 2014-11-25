package djgcv.ssjp.util.flow;

public abstract class PipeImpl<T, R extends Receiver<? super T>> implements Pipe<T, R> {
  private final ReceiverList<R> output;

  protected PipeImpl(ReceiverList<R> output) {
    this.output = output;
  }

  protected PipeImpl() {
    this(new ReceiverListImpl<T, R>());
  }

  @Override
  public ReceiverList<R> getOutput() {
    return output;
  }
}

package djgcv.ssjp.util.flow;

public abstract class PipedOutputEndpoint<T> extends AbstractEndpoint<T> {
  private final Pipe<T> outputPipe;

  protected PipedOutputEndpoint(Pipe<T> outputPipe) {
    this.outputPipe = outputPipe;
  }

  protected PipedOutputEndpoint() {
    this(new ListenerPipe<T>());
  }

  @Override
  public ReceiverList<T> getOutput() {
    return getOutputPipe().getOutput();
  }

  public Pipe<T> getOutputPipe() {
    return outputPipe;
  }
}

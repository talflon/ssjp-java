package djgcv.ssjp.util.flow;

public abstract class EndpointImpl<T> extends AbstractEndpoint<T> {
  private final Pipe<T> outputPipe = new ListenerPipe<T>();

  @Override
  public ReceiverList<T> getOutput() {
    return getOutputPipe().getOutput();
  }

  public Pipe<T> getOutputPipe() {
    return outputPipe;
  }
}

package djgcv.ssjp.util.flow;

import djgcv.ssjp.util.SafeCloseableImpl;

public abstract class EndpointImpl<T> extends SafeCloseableImpl implements Endpoint<T> {
  private final Pipe<T> outputPipe = new HandlerPipe<T>();

  @Override
  public ReceiverList<T> getOutput() {
    return getOutputPipe().getOutput();
  }

  public Pipe<T> getOutputPipe() {
    return outputPipe;
  }
}

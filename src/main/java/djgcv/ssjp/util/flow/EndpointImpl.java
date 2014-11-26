package djgcv.ssjp.util.flow;

import djgcv.ssjp.util.SafeCloseableImpl;

public abstract class EndpointImpl<T> extends SafeCloseableImpl implements Endpoint<T> {
  private final HandlerPipe<T> outputPipe = new HandlerPipeImpl<T>();

  @Override
  public ReceiverList<Handler<? super T>> getOutput() {
    return getOutputPipe().getOutput();
  }

  public HandlerPipe<T> getOutputPipe() {
    return outputPipe;
  }
}

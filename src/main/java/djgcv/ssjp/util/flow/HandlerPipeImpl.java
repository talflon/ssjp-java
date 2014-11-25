package djgcv.ssjp.util.flow;

public class HandlerPipeImpl<T> extends AbstractGenericPipe<T, Handler<? super T>> implements HandlerPipe<T> {
  @Override
  public Handler<? super T> getInput() {
    return input;
  }

  private final Handler<T> input = new HandlerImpl<T>() {
    @Override
    public boolean handle(T value) {
      if (Handlers.tryHandle(getOutput().getReceivers(), value)) {
        return true;
      }
      return false;
    }
  };
}

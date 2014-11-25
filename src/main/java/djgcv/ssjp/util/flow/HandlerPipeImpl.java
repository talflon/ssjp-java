package djgcv.ssjp.util.flow;

public class HandlerPipeImpl<T> extends AbstractGenericPipe<T, Handler<? super T>> implements HandlerPipe<T> {
  @Override
  public Handler<? super T> getInput() {
    return input;
  }

  private final Handler<T> input = new HandlerImpl<T>() {
    @Override
    public boolean handle(T value) {
      for (Handler<? super T> handler : getOutput().getReceivers()) {
        if (handler.handle(value)) {
          return true;
        }
      }
      return false;
    }
  };
}

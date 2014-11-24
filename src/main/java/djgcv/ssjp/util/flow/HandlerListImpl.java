package djgcv.ssjp.util.flow;

public class HandlerListImpl<T> extends ReceiverListImpl<T, Handler<? super T>>
    implements HandlerList<T> {
  @Override
  public boolean handle(T value) {
    for (Handler<? super T> handler : getReceivers()) {
      if (handler.handle(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void receive(T value) {
    handle(value);
  }
}

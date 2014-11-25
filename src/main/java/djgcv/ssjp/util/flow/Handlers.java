package djgcv.ssjp.util.flow;

public final class Handlers {
  public static <T> boolean tryHandle(Iterable<Handler<? super T>> handlers, T value) {
    for (Handler<? super T> handler : handlers) {
      if (handler.handle(value)) {
        return true;
      }
    }
    return false;
  }

  public static <T> Handler<T> forReceiver(final Receiver<? super T> receiver, final boolean considerHandled) {
    return new Handler<T>() {
      @Override
      public void receive(T value) {
        receiver.receive(value);
      }

      @Override
      public boolean handle(T value) {
        receiver.receive(value);
        return considerHandled;
      }
    };
  }

  private Handlers() { }
}

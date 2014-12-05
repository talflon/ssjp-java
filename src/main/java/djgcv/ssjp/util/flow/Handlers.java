package djgcv.ssjp.util.flow;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Handlers {
  public static <T> boolean tryHandle(Iterable<Receiver<? super T>> handlers, T value) {
    for (Receiver<? super T> handler : handlers) {
      if (handler.receive(value)) {
        return true;
      }
    }
    return false;
  }

  public static <T> Receiver<T> forReceiver(Receiver<? super T> receiver,
      boolean considerHandled) {
    return new ForReceiver<T>(receiver, considerHandled);
  }

  public static class ForReceiver<T> implements Receiver<T> {
    private final Receiver<? super T> receiver;
    private final boolean considerHandled;

    public ForReceiver(Receiver<? super T> receiver, boolean considerHandled) {
      this.receiver = checkNotNull(receiver);
      this.considerHandled = considerHandled;
    }

    @Override
    public boolean receive(T value) {
      receiver.receive(value);
      return considerHandled;
    }

    @Override
    public int hashCode() {
      return receiver.hashCode() ^ (considerHandled ? 7829 : 6607);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj == null || !(obj instanceof ForReceiver)) {
        return false;
      } else {
        ForReceiver<?> other = (ForReceiver<?>) obj;
        return considerHandled == other.considerHandled
            && receiver.equals(other.receiver);
      }
    }
  }

  private Handlers() { }
}

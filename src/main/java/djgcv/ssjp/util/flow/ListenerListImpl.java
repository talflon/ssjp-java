package djgcv.ssjp.util.flow;

public class ListenerListImpl<T> extends ReceiverListImpl<T, Receiver<T>> implements ListenerList<T> {
  @Override
  public void receive(T value) {
    propagateAll(value);
  }
}

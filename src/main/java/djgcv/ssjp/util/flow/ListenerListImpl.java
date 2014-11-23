package djgcv.ssjp.util.flow;

public class ListenerListImpl<T> extends ReceiverListImpl<Receiver<T>> implements ListenerList<T> {
  @Override
  public void receive(T value) {
    for (Receiver<T> receiver : getReceivers()) {
      receiver.receive(value);
    }
  }
}

package djgcv.ssjp.util.flow;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReceiverListImpl<T> implements ReceiverList<T> {
  private final Collection<Receiver<? super T>> receivers =
      new CopyOnWriteArrayList<Receiver<? super T>>();

  @Override
  public void appendReceiver(Receiver<? super T> receiver) {
    receivers.add(receiver);
  }

  @Override
  public void prependReceiver(Receiver<? super T> receiver) {
    receivers.add(receiver);
  }

  @Override
  public void removeReceiver(Receiver<? super T> receiver) {
    receivers.remove(receiver);
  }

  @Override
  public Iterable<Receiver<? super T>> getReceivers() {
    return receivers;
  }
}
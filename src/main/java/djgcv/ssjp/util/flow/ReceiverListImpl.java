package djgcv.ssjp.util.flow;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReceiverListImpl<R extends Receiver<?>> implements ReceiverList<R> {
  private final Collection<R> receivers = new CopyOnWriteArrayList<R>();

  @Override
  public void appendReceiver(R receiver) {
    receivers.add(receiver);
  }

  @Override
  public void prependReceiver(R receiver) {
    receivers.add(receiver);
  }

  @Override
  public void removeReceiver(R receiver) {
    receivers.remove(receiver);
  }

  @Override
  public Iterable<R> getReceivers() {
    return receivers;
  }
}
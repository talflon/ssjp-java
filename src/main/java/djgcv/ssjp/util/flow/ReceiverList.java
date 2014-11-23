package djgcv.ssjp.util.flow;

public interface ReceiverList<R extends Receiver<?>> {
  void appendReceiver(R receiver);

  void prependReceiver(R receiver);

  void removeReceiver(R receiver);

  Iterable<R> getReceivers();
}

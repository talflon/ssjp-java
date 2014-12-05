package djgcv.ssjp.util.flow;

public interface ReceiverList<T> {
  void appendReceiver(Receiver<? super T> receiver);

  void prependReceiver(Receiver<? super T>  receiver);

  void removeReceiver(Receiver<? super T>  receiver);

  Iterable<Receiver<? super T>> getReceivers();
}

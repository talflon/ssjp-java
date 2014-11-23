package djgcv.ssjp.util.flow;

public interface Receiver<T> {
  void receive(T value);
}

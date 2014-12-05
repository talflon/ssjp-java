package djgcv.ssjp.util.flow;

public interface Receiver<T> {
  boolean receive(T value);
}

package djgcv.ssjp.util.flow;

public interface Handler<T> extends Receiver<T> {
  boolean handle(T value);
}

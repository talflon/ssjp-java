package djgcv.ssjp.util.flow;

public abstract class HandlerImpl<T> implements Handler<T> {
  @Override
  public void receive(T value) {
    handle(value);
  }
}

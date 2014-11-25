package djgcv.ssjp.util.flow;

import com.google.common.util.concurrent.AbstractFuture;

public class FutureHandler<T> extends AbstractFuture<T> implements Handler<T> {
  @Override
  public boolean handle(T value) {
    return set(value);
  }

  @Override
  public void receive(T value) {
    handle(value);
  }
}

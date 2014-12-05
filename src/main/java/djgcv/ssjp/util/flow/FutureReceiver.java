package djgcv.ssjp.util.flow;

import com.google.common.util.concurrent.AbstractFuture;

public class FutureReceiver<T> extends AbstractFuture<T> implements Receiver<T> {
  @Override
  public boolean receive(T value) {
    return set(value);
  }
}

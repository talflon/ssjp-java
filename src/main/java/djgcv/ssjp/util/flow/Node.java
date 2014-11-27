package djgcv.ssjp.util.flow;

import djgcv.ssjp.util.SafeCloseable;

public interface Node<T> extends Handler<T>, SafeCloseable {
  Endpoint<T> connect(Receiver<? super T> upstream);
}

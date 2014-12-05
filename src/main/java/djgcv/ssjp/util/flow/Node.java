package djgcv.ssjp.util.flow;

import djgcv.ssjp.util.SafeCloseable;

public interface Node<T> extends Endpoint<T>, SafeCloseable {
  Endpoint<T> connect();
}

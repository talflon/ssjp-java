package djgcv.ssjp.util.flow;

import djgcv.ssjp.util.SafeCloseable;

public interface Node<T> extends Handler<T>, SafeCloseable {
  Connection<T> connect();

  interface Connection<T> extends SafeCloseable {
    Node<T> getParent();

    Receiver<T> getInput(Receiver<T> upstream);

    ReceiverList<Handler<? super T>> getOutput();
  }
}

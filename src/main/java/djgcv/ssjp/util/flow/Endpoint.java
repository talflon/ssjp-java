package djgcv.ssjp.util.flow;

import djgcv.ssjp.util.SafeCloseable;

public interface Endpoint<T> extends SafeCloseable {
  Receiver<? super T> getInput();

  ReceiverList<T> getOutput();
}
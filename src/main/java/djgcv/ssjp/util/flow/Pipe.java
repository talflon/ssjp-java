package djgcv.ssjp.util.flow;

public interface Pipe<T> {
  Receiver<? super T> getInput();

  ReceiverList<T> getOutput();
}

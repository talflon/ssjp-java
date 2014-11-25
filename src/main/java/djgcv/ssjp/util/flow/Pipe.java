package djgcv.ssjp.util.flow;

public interface Pipe<T, R extends Receiver<? super T>> {
  R getInput();
  ReceiverList<R> getOutput();
}

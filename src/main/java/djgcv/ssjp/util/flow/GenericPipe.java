package djgcv.ssjp.util.flow;

public interface GenericPipe<T, R extends Receiver<? super T>> {
  R getInput();
  ReceiverList<R> getOutput();
}

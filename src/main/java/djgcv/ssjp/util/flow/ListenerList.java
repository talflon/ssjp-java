package djgcv.ssjp.util.flow;

public interface ListenerList<T> extends ReceiverList<Receiver<? super T>>,
    Receiver<T> {

}
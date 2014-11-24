package djgcv.ssjp.util.flow;

public interface HandlerList<T> extends ReceiverList<Handler<? super T>>,
    Handler<T> {

}
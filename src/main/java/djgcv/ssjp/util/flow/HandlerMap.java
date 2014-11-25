package djgcv.ssjp.util.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.collect.Maps;

public abstract class HandlerMap<K, T> extends HandlerImpl<T> {
  private final Map<K, HandlerPipe<T>> map = Maps.newHashMap();

  protected abstract K getKey(T value);

  protected synchronized HandlerPipe<T> getPipe(K key) {
    HandlerPipe<T> pipe = findPipe(key);
    if (pipe == null) {
      pipe = new HandlerPipeImpl<T>();
      map.put(key, pipe);
    }
    return pipe;
  }

  public synchronized ReceiverList<Handler<? super T>> getHandlers(K key) {
    return getPipe(key).getOutput();
  }

  protected synchronized HandlerPipe<T> findPipe(K key) {
    return map.get(checkNotNull(key));
  }

  @Override
  public boolean handle(T value) {
    K key = getKey(value);
    if (key != null) {
      HandlerPipe<T> pipe = findPipe(key);
      if (pipe != null) {
        return pipe.getInput().handle(value);
      }
    }
    return false;
  }
}

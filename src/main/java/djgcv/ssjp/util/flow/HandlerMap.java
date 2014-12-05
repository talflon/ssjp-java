package djgcv.ssjp.util.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.collect.Maps;

public abstract class HandlerMap<K, T> implements Receiver<T> {
  private final Map<K, Pipe<T>> map = Maps.newHashMap();

  protected abstract K getKey(T value);

  protected synchronized Pipe<T> getPipe(K key) {
    Pipe<T> pipe = findPipe(key);
    if (pipe == null) {
      pipe = new HandlerPipeImpl<T>();
      map.put(key, pipe);
    }
    return pipe;
  }

  public synchronized ReceiverList<T> getHandlers(K key) {
    return getPipe(key).getOutput();
  }

  protected synchronized Pipe<T> findPipe(K key) {
    return map.get(checkNotNull(key));
  }

  @Override
  public boolean receive(T value) {
    K key = getKey(value);
    if (key != null) {
      Pipe<T> pipe = findPipe(key);
      if (pipe != null) {
        return pipe.getInput().receive(value);
      }
    }
    return false;
  }
}

package djgcv.ssjp.util.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.collect.Maps;

public abstract class HandlerMap<K, T> extends HandlerImpl<T> {
  private final Map<K, HandlerList<T>> map = Maps.newHashMap();

  protected abstract K getKey(T value);

  public synchronized HandlerList<T> getHandlers(K key) {
    HandlerList<T> handlers = map.get(checkNotNull(key));
    if (handlers == null) {
      handlers = new HandlerListImpl<T>();
      map.put(key, handlers);
    }
    return handlers;
  }

  public synchronized HandlerList<T> findHandlers(K key) {
    return map.get(checkNotNull(key));
  }

  @Override
  public boolean handle(T value) {
    K key = getKey(value);
    if (key != null) {
      HandlerList<T> handlers = findHandlers(key);
      if (handlers != null) {
        return handlers.handle(value);
      }
    }
    return false;
  }
}

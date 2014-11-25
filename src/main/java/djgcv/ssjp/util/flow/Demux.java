package djgcv.ssjp.util.flow;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import djgcv.ssjp.util.SafeCloseable;
import djgcv.ssjp.util.SafeCloseableImpl;

public abstract class Demux<K, T> extends SafeCloseableImpl implements Node<T> {
  private final Map<K, Connection> connections = Maps.newHashMap();

  @Override
  public synchronized Connection connect() {
    K key = getNextKey();
    Connection conn = new Connection(key);
    if (isClosing()) throw new IllegalStateException();
    connections.put(key, conn);
    return conn;
  }

  protected abstract T muxValue(T value, K key);

  protected abstract K getNextKey();

  protected synchronized Connection getConnection(K key) {
    return connections.get(key);
  }

  protected synchronized void removeConnection(Connection conn) {
    if (connections.get(conn.key) == conn) {
      connections.remove(conn.key);
    }
  }

  @Override
  public void receive(T value) {
    handle(value);
  }

  public ListenableFuture<T> sendRequest(Receiver<T> upstream, T message) {
    FutureHandler<T> response = new FutureHandler<T>();
    final Connection conn = connect();
    conn.getOutput().appendReceiver(response);
    response.addListener(new Runnable() {
      @Override
      public void run() {
        conn.close();
      }
    }, MoreExecutors.sameThreadExecutor());
    conn.getInput(upstream).receive(message);
    return response;
  }

  class Connection extends SafeCloseableImpl implements Node.Connection<T> {
    protected final K key;

    Connection(K key) {
      this.key = key;
    }

    @Override
    public Demux<K, T> getParent() {
      return Demux.this;
    }

    @Override
    public Receiver<T> getInput(final Receiver<T> upstream) {
      return new Receiver<T>() {
        @Override
        public void receive(T value) {
          upstream.receive(muxValue(value, key));
        }
      };
    }

    @Override
    public ReceiverList<Handler<? super T>> getOutput() {
      return output;
    }

    private final ReceiverList<Handler<? super T>> output = new ReceiverListImpl<T, Handler<? super T>>();

    @Override
    protected void performClose() {
      removeConnection(this);
    }

    @Override
    public ListeningExecutorService getCloseExecutor() {
      return Demux.this.getCloseExecutor();
    }
  }

  @Override
  protected synchronized void performClose() {
    SafeCloseable[] children = this.connections.values().toArray(new SafeCloseable[this.connections.size()]);
    for (SafeCloseable child : children) {
      closeSafeCloseable(child);
    }
  }
}

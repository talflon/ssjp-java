package djgcv.ssjp.util.flow;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningExecutorService;

import djgcv.ssjp.util.SafeCloseable;

public abstract class Demux<K, T> extends PipedOutputEndpoint<T> implements Node<T> {
  private final Map<K, Connection> connections = Maps.newHashMap();

  protected Demux(Pipe<T> outputPipe) {
    super(outputPipe);
  }

  protected Demux() {
    super();
  }

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

  protected Pipe<T> createConnectionOutputPipe() {
    return new ListenerPipe<T>();
  }

  protected synchronized Connection getConnection(K key) {
    return connections.get(key);
  }

  protected synchronized void removeConnection(Connection conn) {
    if (connections.get(conn.key) == conn) {
      connections.remove(conn.key);
    }
  }

  protected class Connection extends PipedOutputEndpoint<T> {
    protected final K key;
    private final Receiver<T> input;

    Connection(K key) {
      super(createConnectionOutputPipe());
      this.key = key;
      input = new Receiver<T>() {
        @Override
        public boolean receive(T value) {
          return Demux.this.getOutputPipe().getInput().receive(muxValue(value));
        }
      };
    }

    protected T muxValue(T value) {
      return Demux.this.muxValue(value, key);
    }

    @Override
    public Receiver<T> getInput() {
      return input;
    }

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

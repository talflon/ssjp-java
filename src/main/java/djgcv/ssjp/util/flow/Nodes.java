package djgcv.ssjp.util.flow;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public final class Nodes {
  public static <T> ListenableFuture<T> sendRequest(Node<T> node, T message) {
    FutureReceiver<T> response = new FutureReceiver<T>();
    final Endpoint<T> conn = node.connect();
    conn.getOutput().appendReceiver(response);
    response.addListener(new Runnable() {
      @Override
      public void run() {
        conn.close();
      }
    }, MoreExecutors.sameThreadExecutor());
    conn.getInput().receive(message);
    return response;
  }

  private Nodes() { }
}

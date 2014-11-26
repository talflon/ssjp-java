package djgcv.ssjp.util.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import djgcv.ssjp.util.SafeCloseableImpl;

public class SocketPair extends SafeCloseableImpl {
  private final ListenableFuture<Socket> firstSocket, secondSocket;

  public ListenableFuture<Socket> getFirstSocket() {
    return firstSocket;
  }

  public ListenableFuture<Socket> getSecondSocket() {
    return secondSocket;
  }

  public SocketPair(ListeningExecutorService executor) throws IOException {
    final ServerSocket serverSocket = new ServerSocket(0);
    firstSocket = executor.submit(new Callable<Socket>() {
      @Override
      public Socket call() throws Exception {
        Socket socket = new Socket();
        try {
          socket.connect(serverSocket.getLocalSocketAddress());
          return socket;
        } catch (Exception e) {
          closeQuietly(socket);
          throw e;
        }
      }
    });
    secondSocket = executor.submit(new Callable<Socket>() {
      @Override
      public Socket call() throws Exception {
        try {
          return serverSocket.accept();
        } finally {
          closeQuietly(serverSocket);
        }
      }
    });
    FutureCallback<Socket> callback = new FutureCallback<Socket>() {
      @Override
      public void onSuccess(Socket socket) { }

      @Override
      public void onFailure(Throwable cause) {
        close(cause);
        closeQuietly(serverSocket);
      }
    };
    Futures.addCallback(firstSocket, callback);
    Futures.addCallback(secondSocket, callback);
  }

  @Override
  protected void performClose() {
    firstSocket.cancel(true);
    secondSocket.cancel(true);
  }
}

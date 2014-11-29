package djgcv.ssjp.util.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import djgcv.ssjp.util.SafeCloseableImpl;

public class SocketPair extends SafeCloseableImpl {
  static final Logger log = LoggerFactory.getLogger(SocketPair.class);

  private final ListeningExecutorService executor;
  private final ListenableFuture<Socket> firstSocket, secondSocket;

  public ListenableFuture<Socket> getFirstSocket() {
    return firstSocket;
  }

  public ListenableFuture<Socket> getSecondSocket() {
    return secondSocket;
  }

  protected SocketPair(ListeningExecutorService executor,
      ListenableFuture<Socket> firstSocket,
      ListenableFuture<Socket> secondSocket) {
    this.executor = executor;
    this.firstSocket = firstSocket;
    this.secondSocket = secondSocket;
  }

  @Override
  protected ListeningExecutorService getCloseExecutor() {
    return executor;
  }

  protected void init() {
    FutureCallback<Socket> callback = new FutureCallback<Socket>() {
      @Override
      public void onSuccess(Socket socket) {
      }

      @Override
      public void onFailure(Throwable cause) {
        close(cause);
      }
    };
    Futures.addCallback(firstSocket, callback);
    Futures.addCallback(secondSocket, callback);
  }

  public static SocketPair create(ListeningExecutorService executor)
      throws IOException {
    checkNotNull(executor);
    final ServerSocket serverSocket = new ServerSocket(0);
    ListenableFuture<Socket> firstSocket = executor.submit(
        new Callable<Socket>() {
          @Override
          public Socket call() throws Exception {
            Socket clientSocket = new Socket();
            try {
              clientSocket.connect(serverSocket.getLocalSocketAddress());
              return clientSocket;
            } catch (Exception e) {
              try {
                clientSocket.close();
              } catch (IOException ioe) {
                log.debug("Error closing client socket", ioe);
              }
              throw e;
            }
          }
        });
    ListenableFuture<Socket> secondSocket = executor.submit(
        new Callable<Socket>() {
          @Override
          public Socket call() throws Exception {
            try {
              return serverSocket.accept();
            } finally {
              serverSocket.close();
            }
          }
        });
    return new SocketPair(executor, firstSocket, secondSocket);
  }

  private void cleanup(ListenableFuture<Socket> socketFuture) {
    socketFuture.cancel(true);
    try {
      closeQuietly(socketFuture.get());
    } catch (Exception e) {
      log.debug("Error found while closing SocketPair", e);
    }
  }

  @Override
  protected void performClose() {
    cleanup(firstSocket);
    cleanup(secondSocket);
  }
}

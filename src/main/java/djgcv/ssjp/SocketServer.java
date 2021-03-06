package djgcv.ssjp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import djgcv.ssjp.util.ExecutorShop;
import djgcv.ssjp.util.SafeCloseableImpl;
import djgcv.ssjp.util.flow.Endpoint;
import djgcv.ssjp.util.flow.Node;
import djgcv.ssjp.util.flow.Receiver;

public class SocketServer extends SafeCloseableImpl {
  static final Logger log = LoggerFactory.getLogger(SocketServer.class);

  private final ObjectMapper mapper;
  private final ServerSocket serverSocket;
  private final Node<ObjectNode> node;
  private final ExecutorShop executorShop;
  private final ObjectNode options;

  public SocketServer(ObjectMapper mapper, ServerSocket serverSocket,
      Node<ObjectNode> node, ExecutorShop executorShop,
      ObjectNode options) {
    this.mapper = mapper;
    this.serverSocket = serverSocket;
    this.node = node;
    this.executorShop = executorShop;
    this.options = options;
  }

  public void start() {
    getExecutorShop().getBlockingExecutor().execute(new Runnable() {
      @Override
      public void run() {
        try {
          log.debug("Socket server started");
          while (true) {
            acceptOne();
          }
        } catch (Exception e) {
          log.debug("Stopped listening due to exception", e);
          close(e);
        }
      }
    });
  }

  public ServerSocket getServerSocket() {
    return serverSocket;
  }

  public ExecutorShop getExecutorShop() {
    return executorShop;
  }

  public Node<ObjectNode> getNode() {
    return node;
  }

  protected void acceptOne() throws IOException {
    Socket socket = getServerSocket().accept();
    log.debug("Accepted connection");
    final Endpoint<ObjectNode> localEndpoint = getNode().connect();
    final SsjpServerEndpoint remoteEndpoint = new SsjpServerEndpoint(
        mapper, getExecutorShop(), socket, options) {
      @Override
      protected void performClose() {
        super.performClose();
        closeSafeCloseable(localEndpoint);
      }
    };
    Futures.addCallback(remoteEndpoint.getInputFuture(), new FutureCallback<Receiver<? super ObjectNode>>() {
      @Override
      public void onFailure(Throwable cause) { }

      @Override
      public void onSuccess(Receiver<? super ObjectNode> input) {
        localEndpoint.getOutput().appendReceiver(input);
        remoteEndpoint.getOutput().appendReceiver(localEndpoint.getInput());
        log.debug("Finished connecting");
      }
    });
    remoteEndpoint.start();
  }

  @Override
  protected void performClose() {
    closeQuietly(getServerSocket());
  }
}

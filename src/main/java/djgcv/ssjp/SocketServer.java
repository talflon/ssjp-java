package djgcv.ssjp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import djgcv.ssjp.util.SafeCloseableImpl;
import djgcv.ssjp.util.flow.Endpoint;
import djgcv.ssjp.util.flow.Handlers;
import djgcv.ssjp.util.flow.Node;
import djgcv.ssjp.util.flow.Receiver;

public class SocketServer extends SafeCloseableImpl {
  static final Logger log = LoggerFactory.getLogger(SocketServer.class);

  private final ObjectMapper mapper;
  private final ServerSocket serverSocket;
  private final Node<ObjectNode> node;
  private final ScheduledExecutorService executor;
  private final ObjectNode options;
  private final Receiver<? super ObjectNode> upstream;

  public SocketServer(ObjectMapper mapper, ServerSocket serverSocket,
      Node<ObjectNode> node, ScheduledExecutorService executor,
      ObjectNode options, Receiver<? super ObjectNode> upstream) {
    this.mapper = mapper;
    this.serverSocket = serverSocket;
    this.node = node;
    this.executor = executor;
    this.options = options;
    this.upstream = upstream;
    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          log.debug("Socket server started");
          while (true) {
            acceptOne();
          }
        } catch (Exception e) {
          close(e);
        }
      }
    });
  }

  public ServerSocket getServerSocket() {
    return serverSocket;
  }

  public Executor getExecutor() {
    return executor;
  }

  public Node<ObjectNode> getNode() {
    return node;
  }

  protected void acceptOne() throws IOException {
    Socket socket = getServerSocket().accept();
    log.debug("Accepted connection");
    final Endpoint<ObjectNode> localEndpoint = getNode().connect(upstream);
    final SsjpServerEndpoint remoteEndpoint = new SsjpServerEndpoint(mapper, executor, socket, options) {
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
        localEndpoint.getOutput().appendReceiver(Handlers.forReceiver(input, true));
        remoteEndpoint.getOutput().appendReceiver(Handlers.forReceiver(localEndpoint.getInput(), true));
        log.debug("Finished connecting");
      }
    });
  }

  @Override
  protected void performClose() {
    closeQuietly(getServerSocket());
  }
}

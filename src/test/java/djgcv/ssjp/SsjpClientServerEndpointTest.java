package djgcv.ssjp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import djgcv.ssjp.util.SafeCloseable;
import djgcv.ssjp.util.flow.FutureHandler;
import djgcv.ssjp.util.io.SocketPair;

public class SsjpClientServerEndpointTest {
  ObjectMapper mapper;
  ListeningScheduledExecutorService executor;
  SocketPair socketPair;
  SsjpServerEndpoint server;
  SsjpClientEndpoint client;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    executor = MoreExecutors.listeningDecorator(
        Executors.newScheduledThreadPool(5));
    socketPair = new SocketPair(executor);
  }

  @After
  public void tearDown() throws Exception {
    if (executor != null) {
      executor.shutdownNow();
    }
    if (socketPair != null) {
      socketPair.close();
    }
    if (server != null) {
      server.close();
    }
    if (client != null) {
      client.close();
    }
  }

  protected void startConnect() throws Exception {
    server = new SsjpServerEndpoint(mapper, executor, socketPair.getFirstSocket().get(5, TimeUnit.SECONDS), null);
    client = new SsjpClientEndpoint(mapper, executor, socketPair.getSecondSocket().get(1, TimeUnit.SECONDS), null);
  }

  protected void waitConnected() throws Exception {
    server.getInputFuture().get(5, TimeUnit.SECONDS);
    client.getInputFuture().get(2, TimeUnit.SECONDS);
  }

  @Test
  public void testConnectAndSend() throws Exception {
    startConnect();
    waitConnected();
    ObjectNode message = mapper.createObjectNode();
    FutureHandler<ObjectNode> result = new FutureHandler<ObjectNode>();
    client.getOutput().appendReceiver(result);
    server.getInputFuture().get(5, TimeUnit.SECONDS).receive(message);
    client.getInputFuture().get(2, TimeUnit.SECONDS);
    assertEquals(message, result.get(5, TimeUnit.SECONDS));
  }

  private ListenableFuture<?> waitClosing(SafeCloseable what, long delay,
      TimeUnit unit) throws InterruptedException {
    for (long remaining = unit.toMillis(delay); remaining > 0; remaining--) {
      ListenableFuture<?> future = what.getCloseFuture();
      if (future != null) {
        return future;
      }
      Thread.sleep(1);
    }
    fail("Didn't start closing");
    return null;
  }

  @Test
  public void testTimeout() throws Exception {
    startConnect();
    waitConnected();
    server.setInputTimeout(100, TimeUnit.MILLISECONDS);
    Thread.sleep(20);
    assertFalse(server.isClosing());
    waitClosing(server, 1, TimeUnit.SECONDS);
  }

  @Test
  public void testTimeoutDelayedByKeepalive() throws Exception {
    startConnect();
    waitConnected();
    client.setInputTimeout(200, TimeUnit.MILLISECONDS);
    server.setOutputTimeout(100, TimeUnit.MILLISECONDS);
    Thread.sleep(500);
    assertFalse(server.isClosing());
    assertFalse(client.isClosing());
  }

  @Test
  public void testTimeoutDelayedByReceive() throws Exception {
    startConnect();
    waitConnected();
    ObjectNode message = mapper.createObjectNode();
    server.setInputTimeout(200, TimeUnit.MILLISECONDS);
    Thread.sleep(150);
    client.getInput().receive(message);
    Thread.sleep(100);
    assertFalse(server.isClosing());
    waitClosing(server, 1, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseAfterConnected() throws Exception {
    startConnect();
    waitConnected();
    server.close().get(2, TimeUnit.SECONDS);
    waitClosing(client, 2, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseClientDuringConnect() throws Exception {
    startConnect();
    client.close().get(2, TimeUnit.SECONDS);
    waitClosing(server, 2, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseServerDuringConnect() throws Exception {
    startConnect();
    server.close().get(2, TimeUnit.SECONDS);
    waitClosing(client, 2, TimeUnit.SECONDS);
  }
}

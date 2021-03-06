package djgcv.ssjp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;

import djgcv.ssjp.util.ExecutorShopBase;
import djgcv.ssjp.util.SafeCloseable;
import djgcv.ssjp.util.flow.FutureReceiver;
import djgcv.ssjp.util.io.SocketPair;

public class SsjpClientServerEndpointTest extends ExecutorShopBase {
  static final Logger log = LoggerFactory.getLogger(SsjpClientServerEndpointTest.class);

  ObjectMapper mapper;
  SocketPair socketPair;
  SsjpServerEndpoint server;
  SsjpClientEndpoint client;

  @Before
  public void setUp() throws Exception {
    setExecutorShop();
    mapper = new ObjectMapper();
    socketPair = SocketPair.create(getExecutorShop().getBlockingExecutor());
  }

  @Override
  protected void performClose() {
    super.performClose();
    cleanupSafeCloseable(socketPair);
    cleanupSafeCloseable(server);
    cleanupSafeCloseable(client);
  }

  protected void startConnect() throws Exception {
    server = new SsjpServerEndpoint(mapper, getExecutorShop(),
        socketPair.getFirstSocket().get(5, TimeUnit.SECONDS), null);
    client = new SsjpClientEndpoint(mapper, getExecutorShop(),
        socketPair.getSecondSocket().get(1, TimeUnit.SECONDS), null);
    server.start();
    client.start();
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
    FutureReceiver<ObjectNode> result = new FutureReceiver<ObjectNode>();
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
    server.getInputTimeout().set(100, TimeUnit.MILLISECONDS);
    Thread.sleep(20);
    assertFalse(server.isClosing());
    waitClosing(server, 1, TimeUnit.SECONDS);
  }

  @Test
  public void testTimeoutDelayedByKeepalive() throws Exception {
    startConnect();
    waitConnected();
    client.getInputTimeout().set(200, TimeUnit.MILLISECONDS);
    server.getOutputTimeout().set(100, TimeUnit.MILLISECONDS);
    Thread.sleep(500);
    assertFalse(server.isClosing());
    assertFalse(client.isClosing());
  }

  @Test
  public void testTimeoutDelayedByReceive() throws Exception {
    startConnect();
    waitConnected();
    ObjectNode message = mapper.createObjectNode();
    server.getInputTimeout().set(200, TimeUnit.MILLISECONDS);
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

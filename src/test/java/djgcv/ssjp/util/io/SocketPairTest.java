package djgcv.ssjp.util.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import djgcv.ssjp.util.ExecutorShopBase;

public class SocketPairTest extends ExecutorShopBase {
  SocketPair socketPair;

  @Before
  public void setUp() throws Exception {
    setExecutorShop();
    socketPair = SocketPair.create(getExecutorShop().getBlockingExecutor());
  }

  @Override
  protected void performClose() {
    super.performClose();
    cleanupSafeCloseable(socketPair);
  }

  @Test
  public void testConnect() throws Exception {
    Socket socket = socketPair.getFirstSocket().get(5, TimeUnit.SECONDS);
    assertTrue(socket.isConnected());
    assertFalse(socket.isClosed());
    socket = socketPair.getSecondSocket().get(1, TimeUnit.SECONDS);
    assertTrue(socket.isConnected());
    assertFalse(socket.isClosed());
  }

  @Test
  public void testClose() throws Exception {
    socketPair.close().get(2, TimeUnit.SECONDS);
  }
}

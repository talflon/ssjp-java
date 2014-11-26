package djgcv.ssjp.util.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class SocketPairTest {
  ListeningExecutorService executor;
  SocketPair socketPair;

  @Before
  public void setUp() throws Exception {
    executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
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

package djgcv.ssjp;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import djgcv.ssjp.util.flow.FutureHandler;
import djgcv.ssjp.util.io.SocketPair;

public class SsjpClientServerEndpointTest {
  ObjectMapper mapper;
  ListeningExecutorService executor;
  SocketPair socketPair;
  SsjpServerEndpoint server;
  SsjpClientEndpoint client;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
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
    if (server != null) {
      server.close();
    }
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void test() throws Exception {
    server = new SsjpServerEndpoint(mapper, executor, socketPair.getFirstSocket().get(5, TimeUnit.SECONDS), null);
    client = new SsjpClientEndpoint(mapper, executor, socketPair.getSecondSocket().get(1, TimeUnit.SECONDS), null);
    ObjectNode message = mapper.createObjectNode();
    FutureHandler<ObjectNode> result = new FutureHandler<ObjectNode>();
    client.getOutput().appendReceiver(result);
    server.getInputFuture().get(5, TimeUnit.SECONDS).receive(message);
    client.getInputFuture().get(2, TimeUnit.SECONDS);
    assertEquals(message, result.get(5, TimeUnit.SECONDS));
  }
}

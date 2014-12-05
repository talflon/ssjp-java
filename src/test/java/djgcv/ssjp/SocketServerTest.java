package djgcv.ssjp;

import static org.junit.Assert.assertEquals;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import djgcv.ssjp.util.ExecutorShopBase;
import djgcv.ssjp.util.flow.FutureHandler;
import djgcv.ssjp.util.flow.Handlers;
import djgcv.ssjp.util.flow.Receiver;

public class SocketServerTest extends ExecutorShopBase {
  static final Logger log = LoggerFactory.getLogger(SocketServerTest.class);

  ObjectMapper mapper;
  ServerSocket serverSocket;
  SocketServer server;
  MessageIdDemux demux;
  SsjpClientEndpoint client;

  @Before
  public void setUp() throws Exception {
    setExecutorShop();
    mapper = new ObjectMapper();
    demux = new MessageIdDemux();
    serverSocket = new ServerSocket(0);
    server = new SocketServer(mapper, serverSocket, demux, getExecutorShop(),
        null);
    server.start();
    Socket socket = new Socket();
    socket.connect(serverSocket.getLocalSocketAddress());
    client = new SsjpClientEndpoint(mapper, getExecutorShop(), socket, null);
    client.start();
    client.getInputFuture().get(5, TimeUnit.SECONDS);
  }

  @Override
  protected void performClose() {
    cleanupSafeCloseable(server);
    cleanupQuietly(serverSocket);
    cleanupSafeCloseable(client);
    cleanupSafeCloseable(demux);
  }

  @Test
  public void testClose() throws Exception {
    server.close().get(5, TimeUnit.SECONDS);
  }

  @Test
  public void testConnect() throws Exception {
    waitClientConnect();
  }

  protected void waitClientConnect() throws Exception {
    client.getInputFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  public void testSendReceive() throws Exception {
    waitClientConnect();
    final JsonNode response = new TextNode("thank you for your inquiry");
    demux.getOutput().appendReceiver(Handlers.forReceiver(new Receiver<ObjectNode>() {
      @Override
      public void receive(ObjectNode value) {
        demux.getInput().receive(Messages.response(mapper, response, value.get("tag")));
      }
    }, true));
    FutureHandler<ObjectNode> result = new FutureHandler<ObjectNode>();
    client.getOutput().appendReceiver(result);
    client.getInput().receive(Messages.request(mapper, "com.org.net", "hey_guys"));
    assertEquals(response, result.get(2, TimeUnit.SECONDS).get("rsp"));
  }

  @Test
  public void testSend() throws Exception {
    waitClientConnect();
    String request = "hey_guys";
    FutureHandler<ObjectNode> result = new FutureHandler<ObjectNode>();
    demux.getOutput().appendReceiver(result);
    client.getInput().receive(Messages.request(mapper, "com.org.net", request));
    assertEquals(request, result.get(2, TimeUnit.SECONDS).get("req").asText());
  }
}

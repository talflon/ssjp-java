package djgcv.ssjp;

import static org.junit.Assert.assertEquals;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import djgcv.ssjp.util.flow.FutureHandler;
import djgcv.ssjp.util.flow.Pipe;
import djgcv.ssjp.util.flow.PipeImpl;
import djgcv.ssjp.util.flow.Receiver;

public class SocketServerTest {
  ObjectMapper mapper;
  ServerSocket serverSocket;
  SocketServer server;
  ScheduledExecutorService executor;
  MessageIdDemux demux;
  Pipe<ObjectNode> upstream;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    demux = new MessageIdDemux();
    executor = Executors.newScheduledThreadPool(5);
    upstream = new PipeImpl<ObjectNode>();
    serverSocket = new ServerSocket(0);
    server = new SocketServer(mapper, serverSocket, demux, executor, null, upstream.getInput());
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.close().get(1, TimeUnit.SECONDS);
    }
    if (demux != null) {
      demux.close().get(1, TimeUnit.SECONDS);
    }
    if (executor != null) {
      executor.shutdown();
      if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    }
    if (serverSocket != null) {
      serverSocket.close();
    }
  }

  @Test
  public void testConnect() throws Exception {
    connectClient();
  }

  protected SsjpEndpoint connectClient() throws Exception {
    Socket socket = new Socket();
    socket.connect(serverSocket.getLocalSocketAddress());
    SsjpEndpoint client = new SsjpClientEndpoint(mapper, executor, socket, null);
    client.getInputFuture().get(5, TimeUnit.SECONDS);
    return client;
  }

  @Test
  public void testSendReceive() throws Exception {
    SsjpEndpoint client = connectClient();
    final JsonNode response = new TextNode("thank you for your inquiry");
    upstream.getOutput().appendReceiver(new Receiver<ObjectNode>() {
      @Override
      public void receive(ObjectNode value) {
        System.out.println("Got " + value);
        demux.receive(Messages.response(mapper, response, value.get("tag")));
      }
    });
    FutureHandler<ObjectNode> result = new FutureHandler<ObjectNode>();
    client.getOutput().appendReceiver(result);
    client.getInput().receive(Messages.request(mapper, "com.org.net", "hey_guys"));
    assertEquals(response, result.get(2, TimeUnit.SECONDS).get("rsp"));
  }

  @Test
  public void testSend() throws Exception {
    SsjpEndpoint client = connectClient();
    String request = "hey_guys";
    FutureHandler<ObjectNode> result = new FutureHandler<ObjectNode>();
    upstream.getOutput().appendReceiver(result);
    client.getInput().receive(Messages.request(mapper, "com.org.net", request));
    assertEquals(request, result.get(2, TimeUnit.SECONDS).get("req").asText());
  }
}

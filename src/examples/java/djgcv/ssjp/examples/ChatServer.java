package djgcv.ssjp.examples;

import java.io.IOException;
import java.net.ServerSocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import djgcv.ssjp.HandlerPathMap;
import djgcv.ssjp.HandlerRequestMap;
import djgcv.ssjp.MessageIdDemux;
import djgcv.ssjp.Messages;
import djgcv.ssjp.SocketServer;
import djgcv.ssjp.util.ExecutorShop;
import djgcv.ssjp.util.ExecutorShops;
import djgcv.ssjp.util.flow.ConcurrentPipe;
import djgcv.ssjp.util.flow.Pipe;
import djgcv.ssjp.util.flow.Receiver;

public class ChatServer {
  public static final String PATH = "djgcv.ssjp.examples.chat";
  public static final int PORT = 9470;

  protected static final ObjectMapper mapper = new ObjectMapper();

  protected final MessageIdDemux clientDemux = new MessageIdDemux();
  protected final SocketServer socketServer;
  protected final Pipe<ObjectNode> upstreamPipe;
  protected final BiMap<String, Integer> nicks = HashBiMap.create();

  public ChatServer(int port, ExecutorShop executorShop) throws IOException {
    HandlerRequestMap reqMap = new HandlerRequestMap();
    reqMap.getHandlers("nick").appendReceiver(nickHandler);
    reqMap.getHandlers("say").appendReceiver(saidHandler);
    HandlerPathMap pathMap = new HandlerPathMap();
    pathMap.getHandlers(PATH).appendReceiver(reqMap);
    upstreamPipe = new ConcurrentPipe<ObjectNode>(executorShop.getExecutor());
    upstreamPipe.getOutput().appendReceiver(pathMap);
    clientDemux.getOutput().appendReceiver(upstreamPipe.getInput());
    socketServer = new SocketServer(mapper, new ServerSocket(port),
        clientDemux, executorShop, null);
  }

  protected final Receiver<ObjectNode> nickHandler = new Receiver<ObjectNode>() {
    @Override
    public boolean receive(ObjectNode msg) {
      String nick = msg.path("arg").path("nick").textValue();
      if (nick == null) {
        return false;
      }
      JsonNode idNode = msg.path("tag").path("id");
      if (!idNode.isInt()) {
        return false;
      }
      if (setNick(nick, idNode.asInt())) {
        clientDemux.getInput().receive(Messages.response(
            mapper,
            mapper.createObjectNode()
                .put("success", true),
            msg.get("tag")));
      } else {
        clientDemux.getInput().receive(Messages.response(
            mapper,
            mapper.createObjectNode()
                .put("success", false)
                .put("why", "Nick already taken"),
            msg.get("tag")));
      }
      return true;
    }
  };

  protected final Receiver<ObjectNode> saidHandler = new Receiver<ObjectNode>() {
    @Override
    public boolean receive(ObjectNode msg) {
      String said = msg.path("arg").path("what").textValue();
      if (said == null) {
        return false;
      }
      JsonNode idNode = msg.path("tag").path("id");
      if (!idNode.isInt()) {
        return false;
      }
      propagateSaid(said, idNode.asInt());
      return true;
    }
  };

  protected synchronized boolean setNick(String nick, int id) {
    if (nicks.containsKey(nick)) {
      return false;
    } else {
      nicks.forcePut(nick, id);
      return true;
    }
  }

  protected synchronized void propagateSaid(String what, int id) {
    String nick = nicks.inverse().get(id);
    if (nick == null) {
      nick = "{" + id + "}";
    }
    for (int recvId : nicks.values()) {
      clientDemux.getInput().receive(Messages.request(mapper, PATH, "said",
          mapper.createObjectNode()
              .put("what", what)
              .put("who", nick),
          mapper.createObjectNode()
              .put("id", recvId)));
    }
  }

  public void start() {
    socketServer.start();
  }

  public static void main(String[] args) throws IOException {
    new ChatServer(PORT, ExecutorShops.create()).start();
  }
}

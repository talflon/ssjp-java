package djgcv.ssjp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.Demux;
import djgcv.ssjp.util.flow.Pipe;
import djgcv.ssjp.util.flow.Receiver;

public class MessageIdDemux extends Demux<Integer, ObjectNode> {
  private int nextId = 0;

  public MessageIdDemux(Pipe<ObjectNode> outputPipe) {
    super(outputPipe);
  }

  public MessageIdDemux() {
    super();
  }

  @Override
  protected synchronized Integer getNextKey() {
    while (getConnection(nextId) != null) {
      nextId++;
    }
    return nextId++;
  }

  @Override
  protected ObjectNode muxValue(ObjectNode message, Integer id) {
    ObjectNode wrapped = message.objectNode();
    wrapped.setAll(message);
    ObjectNode newTag = wrapped.putObject("tag").put("id", id);
    if (message.has("tag")) {
      newTag.set("old", message.get("tag"));
    }
    return wrapped;
  }

  @Override
  public Receiver<? super ObjectNode> getInput() {
    return inputHandler;
  }

  private final Receiver<ObjectNode> inputHandler = new Receiver<ObjectNode>() {
    @Override
    public boolean receive(ObjectNode message) {
      JsonNode tag = message.get("tag");
      JsonNode idNode = tag.get("id");
      if (idNode.isInt()) {
        Demux<?, ObjectNode>.Connection connection = getConnection(idNode.asInt());
        if (connection != null) {
          ObjectNode result = message.objectNode();
          result.setAll(message);
          if (tag.has("old")) {
            result.replace("tag", tag.get("old"));
          } else {
            result.remove("tag");
          }
          return connection.getOutputPipe().getInput().receive(result);
        }
      }
      return false;
    }
  };
}

package djgcv.ssjp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.Demux;

public class MessageIdDemux extends Demux<Integer, ObjectNode> {
  private int nextId = 0;

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
  public boolean handle(ObjectNode message) {
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
        return connection.getOutputPipe().getInput().handle(result);
      }
    }
    return false;
  }
}

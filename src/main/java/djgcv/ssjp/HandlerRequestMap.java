package djgcv.ssjp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.HandlerMap;

public class HandlerRequestMap extends HandlerMap<String, ObjectNode> {
  @Override
  protected String getKey(ObjectNode value) {
    JsonNode reqNode = value.get("req");
    if (reqNode != null && reqNode.isTextual()) {
      return reqNode.asText();
    } else {
      return null;
    }
  }
}

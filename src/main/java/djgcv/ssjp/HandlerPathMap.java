package djgcv.ssjp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.HandlerMap;

public class HandlerPathMap extends HandlerMap<String, ObjectNode> {
  @Override
  protected String getKey(ObjectNode value) {
    JsonNode pathNode = value.get("path");
    if (pathNode != null && pathNode.isTextual()) {
      return pathNode.asText();
    } else {
      return null;
    }
  }
}

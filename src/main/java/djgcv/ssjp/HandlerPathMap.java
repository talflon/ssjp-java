package djgcv.ssjp;

import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.HandlerMap;

public class HandlerPathMap extends HandlerMap<String, ObjectNode> {
  @Override
  protected String getKey(ObjectNode value) {
    return value.path("path").textValue();
  }
}

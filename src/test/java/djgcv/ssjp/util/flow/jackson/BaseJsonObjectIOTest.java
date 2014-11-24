package djgcv.ssjp.util.flow.jackson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.io.InputterTest;

abstract class BaseJsonObjectIOTest {
  static final ObjectMapper mapper = new ObjectMapper();

  static List<ObjectNode> getExampleObjects() {
    List<ObjectNode> objects = new ArrayList<ObjectNode>();
    objects.add(mapper.createObjectNode());
    ObjectNode value = mapper.createObjectNode();
    value.with("x").put("y", "z");
    objects.add(value);
    objects.add(mapper.createObjectNode().put("blah", 13));
    value = mapper.createObjectNode();
    ArrayNode arrayNode = value.withArray("stuff");
    arrayNode.add(12.7);
    arrayNode.add(true);
    arrayNode.add("split\nlines");
    objects.add(value);
    return objects;
  }

  static JsonObjectInputter getInputter(byte[] bytes) throws IOException {
    ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
    return new JsonObjectInputter(mapper.getFactory(), bytesIn);
  }

  static void inputAndCheckOne(JsonObjectInputter jsonIn, ObjectNode value)
      throws Exception {
    InputterTest.inputAndCheckOne(jsonIn, value);
  }
}

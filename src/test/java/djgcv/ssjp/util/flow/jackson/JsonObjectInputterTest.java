package djgcv.ssjp.util.flow.jackson;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;

public class JsonObjectInputterTest extends BaseJsonObjectIOTest {
  @Test
  public void testInputOneValue() throws Exception {
    for (ObjectNode msg : getExampleObjects()) {
      inputAndCheckOne(getInputter(msg.toString().getBytes(Charsets.UTF_8)),
          msg);
    }
  }

  @Test
  public void testInputManyValues() throws Exception {
    StringBuilder s = new StringBuilder();
    List<ObjectNode> objects = getExampleObjects();
    for (ObjectNode msg : objects) {
      s.append(msg.toString());
      s.append('\n');
    }
    JsonObjectInputter inputter = getInputter(
        s.toString().getBytes(Charsets.UTF_8));
    for (ObjectNode msg : objects) {
      inputAndCheckOne(inputter, msg);
    }
  }
}

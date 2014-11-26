package djgcv.ssjp.util.flow.jackson;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;

public class JsonObjectOutputterTest extends BaseJsonObjectIOTest {
  private ByteArrayOutputStream bytesOut;
  private JsonObjectOutputter jsonOut;

  @Before
  public void setUp() throws Exception {
    bytesOut = new ByteArrayOutputStream();
    jsonOut = new JsonObjectOutputter(mapper.getFactory(), bytesOut, false);
  }

  @Test
  public void testOutputOne() throws Exception {
    for (ObjectNode value : getExampleObjects()) {
      setUp();
      jsonOut.receive(value);
      inputAndCheckOne(getInputter(bytesOut.toByteArray()), value);
    }
  }

  @Test
  public void testOutputMany() throws Exception {
    List<ObjectNode> objects = getExampleObjects();
    for (ObjectNode value : objects) {
      jsonOut.receive(value);
    }
    JsonObjectInputter jsonIn = getInputter(bytesOut.toByteArray());
    for (ObjectNode value : objects) {
      inputAndCheckOne(jsonIn, value);
    }
  }

  @Test
  public void testOutputLineBreaks() throws Exception {
    List<ObjectNode> objects = getExampleObjects();
    for (ObjectNode value : objects) {
      jsonOut.receive(value);
    }
    String allOutput = new String(bytesOut.toByteArray(), Charsets.UTF_8);
    String[] results = allOutput.split("\n");
    int i = 0;
    for (ObjectNode value : objects) {
      inputAndCheckOne(getInputter(results[i++].getBytes(Charsets.UTF_8)),
          value);
    }
  }

  @Test
  public void testKeepalive() throws Exception {
    ObjectNode testValue = mapper.createObjectNode();
    for (int n = 1; n <= 5; n++) {
      setUp();
      for (int i = 0; i < n; i++) {
        jsonOut.receive(JsonObjectOutputter.KEEPALIVE);
      }
      byte[] expected = new byte[n];
      Arrays.fill(expected, (byte) '\n');
      assertArrayEquals(expected, bytesOut.toByteArray());
      jsonOut.receive(testValue);
      inputAndCheckOne(getInputter(bytesOut.toByteArray()), testValue);
    }
  }
}

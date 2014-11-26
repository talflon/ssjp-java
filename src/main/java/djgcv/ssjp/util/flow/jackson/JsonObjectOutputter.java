package djgcv.ssjp.util.flow.jackson;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.io.Outputter;

public class JsonObjectOutputter extends Outputter<ObjectNode> {
  public static final ObjectNode KEEPALIVE =
      new ObjectNode(new JsonNodeFactory(false));

  private final JsonGenerator generator;

  public JsonObjectOutputter(JsonFactory factory, OutputStream outputStream,
      boolean manageOutputStream) throws IOException {
    super(outputStream, manageOutputStream);
    this.generator = factory.createGenerator(getOutputStream());
  }

  @Override
  protected void outputValue(ObjectNode value) throws IOException {
    if (value != KEEPALIVE) {
      generator.writeTree(value);
    }
    getOutputStream().write('\n');
  }

  @Override
  protected void performClose() {
    super.performClose();
    closeQuietly(generator);
  }
}

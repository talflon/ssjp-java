package djgcv.ssjp.util.flow.jackson;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.io.Inputter;

public class JsonObjectInputter extends Inputter<ObjectNode> {
  private final JsonFactory factory;
  private JsonParser parser;

  public JsonObjectInputter(JsonFactory factory, InputStream inputStream,
      boolean manageInputStream) {
    super(inputStream, manageInputStream);
    this.factory = factory;
  }

  @Override
  protected ObjectNode readOneValue() throws IOException {
    if (parser == null) {
      parser = factory.createParser(getInputStream());
    }
    JsonNode value = parser.readValueAsTree();
    if (value == null) {
      throw new EOFException();
    } else if (value.isObject()) {
      return (ObjectNode) value;
    } else {
      throw new IOException("Wrong type of JSON data: " + value.getNodeType());
    }
  }

  @Override
  protected void performClose() {
    super.performClose();
    if (parser != null) {
      closeQuietly(parser);
    }
  }
}

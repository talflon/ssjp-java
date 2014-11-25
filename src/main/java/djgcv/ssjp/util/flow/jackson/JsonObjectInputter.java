package djgcv.ssjp.util.flow.jackson;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.io.Inputter;

public class JsonObjectInputter extends Inputter<ObjectNode> {
  private final JsonParser parser;

  public JsonObjectInputter(JsonFactory factory, InputStream inputStream,
      boolean manageInputStream) throws IOException {
    super(inputStream, manageInputStream);
    parser = factory.createParser(getInputStream());
  }

  @Override
  protected ObjectNode readOneValue() throws IOException {
    return parser.readValueAsTree();
  }

  @Override
  protected void performClose() {
    super.performClose();
    closeQuietly(parser);
  }
}

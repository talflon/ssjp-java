package djgcv.ssjp.util.flow.jackson;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.io.Outputter;

public class JsonObjectOutputter extends Outputter<ObjectNode> {
  private final JsonGenerator generator;

  public JsonObjectOutputter(JsonFactory factory, OutputStream outputStream)
      throws IOException {
    super(outputStream);
    this.generator = factory.createGenerator(getOutputStream());
  }

  @Override
  protected void outputValue(ObjectNode value) throws IOException {
    generator.writeTree(value);
    generator.writeRaw('\n');
  }
}

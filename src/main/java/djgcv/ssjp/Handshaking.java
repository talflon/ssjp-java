package djgcv.ssjp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Handshaking {
  public static final String SSJP_VERSION = "1.0";

  public static ObjectNode createGreeting(ObjectMapper mapper,
      ObjectNode options) {
    ObjectNode greeting = mapper.createObjectNode();
    greeting.put("ssjp", SSJP_VERSION);
    Messages.setIfNotNull(greeting, "opt", options);
    return greeting;
  }

  private Handshaking() {
  }
}

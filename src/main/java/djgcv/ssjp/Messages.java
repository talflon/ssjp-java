package djgcv.ssjp;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Messages {
  public static ObjectNode request(ObjectMapper mapper, String path,
      String request) {
    return mapper.createObjectNode().put("path", path).put("req", request);
  }

  public static ObjectNode request(ObjectMapper mapper, String path,
      String request, JsonNode args, JsonNode tag) {
    ObjectNode result = mapper.createObjectNode();
    result.put("path", checkNotNull(path));
    result.put("req", checkNotNull(request));
    setIfNotNull(result, "arg", args);
    setIfNotNull(result, "tag", tag);
    return result;
  }

  public static ObjectNode request(ObjectMapper mapper, String path,
      String request, JsonNode args) {
    return request(mapper, path, request, args, null);
  }

  public static ObjectNode response(ObjectMapper mapper, JsonNode response,
      JsonNode tag) {
    ObjectNode result = mapper.createObjectNode();
    result.set("rsp", checkNotNull(response));
    setIfNotNull(result, "tag", tag);
    return result;
  }

  static void setIfNotNull(ObjectNode objectNode, String key,
      JsonNode value) {
    if (value != null) {
      objectNode.set(key, value);
    }
  }
}

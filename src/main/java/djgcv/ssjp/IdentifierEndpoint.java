package djgcv.ssjp;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.io.BaseEncoding;

import djgcv.ssjp.util.RandomSource;
import djgcv.ssjp.util.ThreadLocalRandomSource;
import djgcv.ssjp.util.flow.Endpoint;
import djgcv.ssjp.util.flow.EndpointImpl;
import djgcv.ssjp.util.flow.HandlerPipe;
import djgcv.ssjp.util.flow.Pipe;
import djgcv.ssjp.util.flow.Receiver;

public class IdentifierEndpoint extends EndpointImpl<ObjectNode> {
  public static final String PATH = "ssjp.iden";
  public static final int NUM_BITS = 96;
  public static final int NUM_CHARS = NUM_BITS / 6;
  public static final int NUM_BYTES = NUM_BITS / 8;

  private static final ObjectMapper mapper = new ObjectMapper();

  private final BiMap<Integer, String> identifierMap = HashBiMap.create();
  private final RandomSource randomSource;
  private final Endpoint<ObjectNode> wrapped;
  private final Pipe<ObjectNode> inputPipe;

  public IdentifierEndpoint(Endpoint<ObjectNode> wrapped,
      RandomSource randomSource) {
    this.wrapped = wrapped;
    this.randomSource = randomSource;

    HandlerRequestMap reqMap = new HandlerRequestMap();
    reqMap.getHandlers("iden?").appendReceiver(new Receiver<ObjectNode>() {
      @Override
      public boolean receive(ObjectNode msg) {
        JsonNode tag = msg.path("tag");
        JsonNode idNode = tag.path("id");
        if (idNode.isInt()) {
          String identifier = getNewIdentifier(idNode.asInt());
          getInput().receive(Messages.response(
              mapper,
              mapper.createObjectNode().put("iden", identifier),
              tag));
          return true;
        } else {
          return false;
        }
      }
    });
    reqMap.getHandlers("iden=").appendReceiver(new Receiver<ObjectNode>() {
      @Override
      public boolean receive(ObjectNode msg) {
        JsonNode tag = msg.path("tag");
        JsonNode idNode = tag.path("id");
        if (idNode.isInt()) {
          JsonNode idenNode = msg.path("arg").path("iden");
          if (idenNode.isTextual()) {
            setIdentifier(idNode.asInt(), idenNode.asText());
            getInput().receive(Messages.response(
                mapper,
                mapper.createObjectNode().put("success", true),
                tag));
            return true;
          }
        }
        return false;
      }
    });

    HandlerPathMap pathMap = new HandlerPathMap();
    pathMap.getHandlers(PATH).appendReceiver(reqMap);
    inputPipe = new HandlerPipe<ObjectNode>();
    inputPipe.getOutput().appendReceiver(pathMap);
    inputPipe.getOutput().appendReceiver(new Receiver<ObjectNode>() {
      @Override
      public boolean receive(ObjectNode msg) {
        JsonNode tag = msg.path("tag");
        JsonNode idNode = tag.path("id");
        if (idNode.isInt()) {
          String iden = getOldIdentifier(idNode.asInt());
          if (iden != null) {
            msg = msg.deepCopy(); // TODO worth avoiding the full copy?
            ((ObjectNode) msg.get("tag")).put("iden", iden);
          }
        }
        return getOutputPipe().getInput().receive(msg);
      }
    });
    wrapped.getOutput().appendReceiver(inputPipe.getInput());
  }

  public IdentifierEndpoint(Endpoint<ObjectNode> wrapped) {
    this(wrapped, ThreadLocalRandomSource.getDefault());
  }

  @Override
  public Receiver<? super ObjectNode> getInput() {
    return wrapped.getInput();
  }

  @Override
  protected void performClose() {
    wrapped.getOutput().removeReceiver(inputPipe.getInput());
  }

  protected void setIdentifier(int id, String identifier) {
    synchronized (identifierMap) {
      identifierMap.forcePut(id, checkNotNull(identifier));
    }
  }

  protected String getNewIdentifier(int id) {
    for (int i = 0; i < 1000; i++) {
      String identifier = generateIdentifier();
      synchronized (identifierMap) {
        if (!identifierMap.containsValue(identifier)) {
          identifierMap.forcePut(id, identifier);
          return identifier;
        }
      }
    }
    throw new RuntimeException("Couldn't do it");
  }

  protected String getOldIdentifier(int id) {
    synchronized (identifierMap) {
      return identifierMap.get(id);
    }
  }

  private static final BaseEncoding encoding =
      BaseEncoding.base64().omitPadding();

  protected String generateIdentifier() {
    byte[] bytes = new byte[NUM_BYTES];
    randomSource.getRandom().nextBytes(bytes);
    return encoding.encode(bytes);
  }
}

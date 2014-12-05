package djgcv.ssjp;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.flow.Nodes;
import djgcv.ssjp.util.flow.Receiver;

public class MessageIdDemuxTest {
  @Test
  public void testSendRequest() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final MessageIdDemux demux = new MessageIdDemux();
    Receiver<ObjectNode> echoServer = new Receiver<ObjectNode>() {
      @Override
      public void receive(ObjectNode message) {
        demux.receive(Messages.response(mapper, message.get("arg"),
            message.get("tag")));
      }
    };
    JsonNode args = mapper.valueToTree("hello!");
    JsonNode tag = mapper.valueToTree("tag");
    ObjectNode response = Nodes.sendRequest(demux, echoServer,
        Messages.request(mapper, "com.echo", "please_echo", args, tag)).get(1,
        TimeUnit.SECONDS);
    assertEquals(args, response.get("rsp"));
    assertEquals(tag, response.get("tag"));
    demux.close().get(1, TimeUnit.SECONDS);
  }
}

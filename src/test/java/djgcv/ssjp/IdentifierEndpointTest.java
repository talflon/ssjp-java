package djgcv.ssjp;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.RandomSource;
import djgcv.ssjp.util.SingleRandomSource;
import djgcv.ssjp.util.flow.Endpoint;
import djgcv.ssjp.util.flow.FutureReceiver;

public class IdentifierEndpointTest {
  ObjectMapper mapper;
  RandomSource randomSource;
  MessageIdDemux demux;
  IdentifierEndpoint identifier;
  Endpoint<ObjectNode> child;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    randomSource = new SingleRandomSource(new Random(654321));
    demux = new MessageIdDemux();
    identifier = new IdentifierEndpoint(demux, randomSource);
    child = demux.connect();
  }

  String getNewIden() throws Exception {
    FutureReceiver<ObjectNode> result = new FutureReceiver<ObjectNode>();
    child.getOutput().appendReceiver(result);
    try {
      child.getInput().receive(Messages.request(mapper,
          IdentifierEndpoint.PATH, "iden?"));
      ObjectNode response = result.get(1, TimeUnit.SECONDS);
      return response.get("rsp").get("iden").textValue();
    } finally {
      child.getOutput().removeReceiver(result);
    }
  }

  String getIdenUsed() throws Exception {
    FutureReceiver<ObjectNode> result = new FutureReceiver<ObjectNode>();
    identifier.getOutput().appendReceiver(result);
    try {
      child.getInput().receive(Messages.request(mapper, "a.b.c", "yo"));
      ObjectNode request = result.get(1, TimeUnit.SECONDS);
      return request.path("tag").path("iden").textValue();
    } finally {
      identifier.getOutput().removeReceiver(result);
    }
  }

  void sendSetIden(String iden) throws Exception {
    child.getInput().receive(Messages.request(mapper,
        IdentifierEndpoint.PATH, "iden=",
        mapper.createObjectNode().put("iden", iden)));
  }

  @Test
  public void testGetNewIdentifier() throws Exception {
    assertEquals(IdentifierEndpoint.NUM_CHARS, getNewIden().length());
  }

  @Test
  public void testNewIdentifierUsed() throws Exception {
    String iden = getNewIden();
    assertEquals(iden, getIdenUsed());
  }

  @Test
  public void testSetIdentifierSuccess() throws Exception {
    String iden = "1234567890abcdef";
    FutureReceiver<ObjectNode> result = new FutureReceiver<ObjectNode>();
    child.getOutput().appendReceiver(result);
    sendSetIden(iden);
    assertEquals(BooleanNode.TRUE,
        result.get(1, TimeUnit.SECONDS).path("rsp").path("success"));
  }

  @Test
  public void testSetIdentifierUsed() throws Exception {
    String iden = "1234567890abcdef";
    sendSetIden(iden);
    assertEquals(iden, getIdenUsed());
  }
}

package djgcv.ssjp;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;

import djgcv.ssjp.util.flow.Endpoint;
import djgcv.ssjp.util.flow.Receiver;

public interface SsjpEndpoint extends Endpoint<ObjectNode> {
  ListenableFuture<Receiver<? super ObjectNode>> getInputFuture();
}

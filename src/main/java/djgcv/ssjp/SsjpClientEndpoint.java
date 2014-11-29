package djgcv.ssjp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import djgcv.ssjp.util.ExecutorShop;

public class SsjpClientEndpoint extends BaseSsjpEndpoint {
  public SsjpClientEndpoint(ObjectMapper mapper, ExecutorShop executorShop,
      InputStream inputStream, OutputStream outputStream, ObjectNode options)
      throws IOException {
    super(mapper, executorShop, inputStream, outputStream, options);
  }

  public SsjpClientEndpoint(ObjectMapper mapper, ExecutorShop executorShop,
      Socket socket, ObjectNode options) throws IOException {
    super(mapper, executorShop, socket, options);
  }

  @Override
  protected void startHandshake() {
    sendOurGreeting();
  }

  @Override
  protected void afterSending() {
    getTheirGreeting();
  }

  @Override
  protected void afterReceiving() {
    finishHandshaking();
  }
}

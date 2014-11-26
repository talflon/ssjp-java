package djgcv.ssjp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SsjpClientEndpoint extends BaseSsjpEndpoint {
  public SsjpClientEndpoint(ObjectMapper mapper, Executor executor,
      InputStream inputStream, OutputStream outputStream, ObjectNode options)
      throws IOException {
    super(mapper, executor, inputStream, outputStream, options);
  }

  public SsjpClientEndpoint(ObjectMapper mapper, Executor executor,
      Socket socket, ObjectNode options) throws IOException {
    super(mapper, executor, socket, options);
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

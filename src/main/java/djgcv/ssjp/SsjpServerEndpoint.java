package djgcv.ssjp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SsjpServerEndpoint extends BaseSsjpEndpoint {
  public SsjpServerEndpoint(ObjectMapper mapper,
      ScheduledExecutorService executor,
      InputStream inputStream, OutputStream outputStream, ObjectNode options)
      throws IOException {
    super(mapper, executor, inputStream, outputStream, options);
  }

  public SsjpServerEndpoint(ObjectMapper mapper,
      ScheduledExecutorService executor,
      Socket socket, ObjectNode options) throws IOException {
    super(mapper, executor, socket, options);
  }

  @Override
  protected void startHandshake() {
    getTheirGreeting();
  }

  @Override
  protected void afterReceiving() {
    sendOurGreeting();
  }

  @Override
  protected void afterSending() {
    finishHandshaking();
  }
}

package djgcv.ssjp.util.flow.io;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import djgcv.ssjp.util.flow.HandlerImpl;

public abstract class Outputter<T> extends HandlerImpl<T> {
  static final Logger log = LoggerFactory.getLogger(Outputter.class);

  private final OutputStream outputStream;

  protected Outputter(OutputStream outStream) {
    this.outputStream = outStream;
  }

  protected abstract void outputValue(T value) throws IOException;

  protected Logger getLogger() {
    return log;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public boolean handle(T value) {
    try {
      outputValue(value);
      getOutputStream().flush();
      return true;
    } catch (IOException e) {
      handleError(value, e);
      return false;
    }
  }

  protected void handleError(T value, IOException e) {
    getLogger().error("Error outputting " + value, e);
  }
}
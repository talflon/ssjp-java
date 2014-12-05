package djgcv.ssjp.util.flow.io;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import djgcv.ssjp.util.SafeCloseableImpl;
import djgcv.ssjp.util.flow.Receiver;

public abstract class Outputter<T> extends SafeCloseableImpl implements
    Receiver<T> {
  static final Logger log = LoggerFactory.getLogger(Outputter.class);

  private final OutputStream outputStream;
  private final boolean manageOutputStream;

  protected Outputter(OutputStream outputStream, boolean manageOutputStream) {
    this.outputStream = outputStream;
    this.manageOutputStream = manageOutputStream;
  }

  protected abstract void outputValue(T value) throws IOException;

  @Override
  protected Logger getLogger() {
    return log;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public boolean receive(T value) {
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

  @Override
  protected void performClose() {
    if (manageOutputStream) {
      closeQuietly(getOutputStream());
    }
  }
}
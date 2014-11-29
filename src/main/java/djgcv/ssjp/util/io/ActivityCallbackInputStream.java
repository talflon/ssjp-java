package djgcv.ssjp.util.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ActivityCallbackInputStream extends FilterInputStream
    implements ActivityCallbackStream {
  private volatile Runnable callback = null;

  public ActivityCallbackInputStream(InputStream in) {
    super(in);
  }

  public ActivityCallbackInputStream(InputStream in, Runnable callback) {
    super(in);
    this.callback = callback;
  }

  @Override
  public Runnable getCallback() {
    return callback;
  }

  @Override
  public void setCallback(Runnable callback) {
    this.callback = callback;
  }

  protected int checkActivity(int result) {
    if (result >= 0) {
      onActivity();
    }
    return result;
  }

  protected void onActivity() {
    Runnable callback = this.callback;
    if (callback != null) {
      callback.run();
    }
  }

  @Override
  public int read() throws IOException {
    return checkActivity(super.read());
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return checkActivity(super.read(b, off, len));
  }

  @Override
  public int read(byte[] b) throws IOException {
    return checkActivity(super.read(b));
  }
}

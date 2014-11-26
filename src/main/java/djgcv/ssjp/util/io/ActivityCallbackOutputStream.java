package djgcv.ssjp.util.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ActivityCallbackOutputStream extends FilterOutputStream {
  private volatile Runnable callback = null;

  public ActivityCallbackOutputStream(OutputStream out) {
    super(out);
  }

  public ActivityCallbackOutputStream(OutputStream out, Runnable callback) {
    super(out);
    this.callback = callback;
  }

  public Runnable getCallback() {
    return callback;
  }

  public void setCallback(Runnable callback) {
    this.callback = callback;
  }

  protected void onActivity() {
    Runnable callback = this.callback;
    if (callback != null) {
      callback.run();
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (len > 0) {
      super.write(b, off, len);
      onActivity();
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    if (b.length > 0) {
      super.write(b);
      onActivity();
    }
  }

  @Override
  public void write(int b) throws IOException {
    super.write(b);
    onActivity();
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    onActivity();
  }
}

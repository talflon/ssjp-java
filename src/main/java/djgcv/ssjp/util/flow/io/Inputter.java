package djgcv.ssjp.util.flow.io;

import java.io.IOException;
import java.io.InputStream;

import djgcv.ssjp.util.flow.Receiver;
import djgcv.ssjp.util.flow.ReceiverListImpl;

public abstract class Inputter<T> extends
    ReceiverListImpl<T, Receiver<? super T>> {
  private final InputStream inputStream;

  protected Inputter(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  protected abstract T readOneValue() throws IOException;

  public void inputOneValue() throws IOException {
    propagateAll(readOneValue());
  }
}

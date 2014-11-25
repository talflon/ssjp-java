package djgcv.ssjp.util.flow.io;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import djgcv.ssjp.util.SafeCloseableImpl;
import djgcv.ssjp.util.flow.Receiver;
import djgcv.ssjp.util.flow.ReceiverList;
import djgcv.ssjp.util.flow.ReceiverListImpl;

public abstract class Inputter<T> extends SafeCloseableImpl {
  static final Logger log = LoggerFactory.getLogger(Inputter.class);

  private final ReceiverListImpl<T, Receiver<? super T>> receiverList = new ReceiverListImpl<T, Receiver<? super T>>();
  private final InputStream inputStream;
  private final boolean manageInputStream;

  protected Inputter(InputStream inputStream, boolean manageInputStream) {
    this.inputStream = inputStream;
    this.manageInputStream = manageInputStream;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  ReceiverList<Receiver<? super T>> getReceiverList() {
    return receiverList;
  }

  protected abstract T readOneValue() throws IOException;

  public void inputOneValue() throws IOException {
    T value = readOneValue();
    for (Receiver<? super T> receiver : getReceiverList().getReceivers()) {
      receiver.receive(value);
    }
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  protected void performClose() {
    if (manageInputStream) {
      closeQuietly(getInputStream());
    }
  }
}

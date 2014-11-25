package djgcv.ssjp.util.flow.io;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import djgcv.ssjp.util.SafeCloseableImpl;
import djgcv.ssjp.util.flow.Receiver;
import djgcv.ssjp.util.flow.ReceiverList;
import djgcv.ssjp.util.flow.ReceiverListImpl;

public abstract class Inputter<T> extends SafeCloseableImpl implements
    ReceiverList<Receiver<? super T>> {
  static final Logger log = LoggerFactory.getLogger(Inputter.class);

  private final ReceiverListImpl<T, Receiver<? super T>> receivers = new ReceiverListImpl<T, Receiver<? super T>>();
  private final InputStream inputStream;
  private final boolean manageInputStream;

  protected Inputter(InputStream inputStream, boolean manageInputStream) {
    this.inputStream = inputStream;
    this.manageInputStream = manageInputStream;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  protected abstract T readOneValue() throws IOException;

  public void inputOneValue() throws IOException {
    receivers.propagateAll(readOneValue());
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  public void appendReceiver(Receiver<? super T> receiver) {
    receivers.appendReceiver(receiver);
  }

  @Override
  public void prependReceiver(Receiver<? super T> receiver) {
    receivers.prependReceiver(receiver);
  }

  @Override
  public void removeReceiver(Receiver<? super T> receiver) {
    receivers.removeReceiver(receiver);
  }

  @Override
  public Iterable<Receiver<? super T>> getReceivers() {
    return receivers.getReceivers();
  }

  @Override
  protected void performClose() {
    if (manageInputStream) {
      closeQuietly(getInputStream());
    }
  }
}

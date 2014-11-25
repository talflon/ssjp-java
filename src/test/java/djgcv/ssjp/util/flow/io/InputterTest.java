package djgcv.ssjp.util.flow.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import djgcv.ssjp.util.flow.Receiver;

public class InputterTest {
  public static <T> void inputAndCheckOne(Inputter<T> inputter, T value)
      throws Exception {
    final SettableFuture<T> future = SettableFuture.create();
    Receiver<T> receiver = new Receiver<T>() {
      @Override
      public void receive(T value) {
        assertTrue("Received value twice", future.set(value));
      }
    };
    inputter.getReceiverList().appendReceiver(receiver);
    inputter.inputOneValue();
    assertEquals(value, future.get(1, TimeUnit.SECONDS));
    inputter.getReceiverList().removeReceiver(receiver);
  }

  @Test
  public void testInputOneValue() throws Exception {
    byte value = (byte) 'Q';
    InputStream inputStream = new ByteArrayInputStream(new byte[] { value });
    Inputter<Byte> inputter = new Inputter<Byte>(inputStream, false) {
      @Override
      protected Byte readOneValue() throws IOException {
        int value = getInputStream().read();
        if (value < 0) {
          throw new EOFException();
        }
        return (byte) value;
      }
    };
    inputAndCheckOne(inputter, value);
  }
}

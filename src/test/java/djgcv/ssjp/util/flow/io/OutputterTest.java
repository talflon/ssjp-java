package djgcv.ssjp.util.flow.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

public class OutputterTest {
  @Test
  public void testHandle() {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    Outputter<Byte> outputter = new Outputter<Byte>(bytesOut, false) {
      @Override
      protected void outputValue(Byte value) throws IOException {
        getOutputStream().write(value);
      }
    };
    byte value = '9';
    assertTrue(outputter.handle(value));
    assertArrayEquals(new byte[] { value }, bytesOut.toByteArray());
  }

  @Test
  public void testHandleFlushes() {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    Outputter<Byte> outputter = new Outputter<Byte>(
        new BufferedOutputStream(bytesOut), false) {
      @Override
      protected void outputValue(Byte value) throws IOException {
        getOutputStream().write(value);
      }
    };
    byte value = 'm';
    assertTrue(outputter.handle(value));
    assertArrayEquals(new byte[] { value }, bytesOut.toByteArray());
  }

  @Test
  public void testHandleError() throws Exception {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    final IOException ioException = new IOException("argh");
    final SettableFuture<IOException> result = SettableFuture.create();
    final SettableFuture<Byte> attempted = SettableFuture.create();
    Outputter<Byte> outputter = new Outputter<Byte>(bytesOut, false) {
      @Override
      protected void outputValue(Byte value) throws IOException {
        throw ioException;
      }

      @Override
      protected void handleError(Byte value, IOException e) {
        result.set(e);
        attempted.set(value);
      }
    };
    byte value = '$';
    assertFalse(outputter.handle(value));
    assertEquals(ioException, result.get(1, TimeUnit.SECONDS));
    assertEquals((Byte) value, attempted.get(1, TimeUnit.SECONDS));
  }
}

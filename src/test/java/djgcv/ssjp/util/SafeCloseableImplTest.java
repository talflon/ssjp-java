package djgcv.ssjp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

public class SafeCloseableImplTest extends ExecutorTestBase<ListeningExecutorService> {
  @Before
  public void setUp() {
    setExecutor(MoreExecutors.listeningDecorator(
        Executors.newCachedThreadPool(this)));
  }

  private class SafeCloseableImpl extends djgcv.ssjp.util.SafeCloseableImpl {
    @Override
    protected void performClose() { }

    @Override
    public ListeningExecutorService getCloseExecutor() {
      return getExecutor();
    }
  }

  @Test
  public void testPerformClose() throws Exception {
    final SettableFuture<?> called = SettableFuture.create();
    SafeCloseableImpl c = new SafeCloseableImpl() {
      @Override
      protected void performClose() {
        called.set(null);
      }
    };
    c.close().get(1, TimeUnit.SECONDS);
    called.get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseThrowable() throws Exception {
    SafeCloseableImpl c = new SafeCloseableImpl();
    final SettableFuture<Throwable> thrown = SettableFuture.create();
    listenLogger(c.getLogger(), new Handler() {
      @Override
      public void publish(LogRecord record) {
        Throwable t = record.getThrown();
        if (t != null) {
          thrown.set(t);
        }
      }

      @Override
      public void flush() { }

      @Override
      public void close() throws SecurityException { }
    });
    Exception exc = new RuntimeException("test");
    c.close(exc).get(1, TimeUnit.SECONDS);
    assertEquals(exc, thrown.get(1, TimeUnit.SECONDS));
  }

  @Test
  public void testClose() throws Exception {
    SafeCloseable c = new SafeCloseableImpl();
    c.close().get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testIsClosing() {
    SafeCloseable c = new SafeCloseableImpl();
    assertFalse(c.isClosing());
    c.close();
    assertTrue(c.isClosing());
  }

  @Test
  public void testIsClosed() throws Exception {
    SafeCloseable c = new SafeCloseableImpl();
    assertFalse(c.isClosed());
    c.close().get(1, TimeUnit.SECONDS);
    assertTrue(c.isClosed());
  }

  @Test
  public void testCloseSafeCloseable() throws Exception {
    final SafeCloseable child = new SafeCloseableImpl();
    SafeCloseable parent = new SafeCloseableImpl() {
      @Override
      protected void performClose() {
        closeSafeCloseable(child);
      }
    };
    assertFalse(child.isClosed());
    assertFalse(child.isClosing());
    parent.close().get(1, TimeUnit.SECONDS);
    assertTrue(child.isClosed());
  }

  @Test
  public void testAddCloseTask() throws Exception {
    final SettableFuture<?> future = SettableFuture.create();
    SafeCloseable parent = new SafeCloseableImpl() {
      @Override
      protected void performClose() {
        addCloseTask(future);
      }
    };
    parent.close();
    assertTrue(parent.isClosing());
    assertFalse(parent.isClosed());
    future.set(null);
    parent.getCloseFuture().get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testRunCloseTask() throws Exception {
    final SettableFuture<?> future = SettableFuture.create();
    SafeCloseable parent = new SafeCloseableImpl() {
      @Override
      protected void performClose() {
        runCloseTask(new Runnable() {
          @Override
          public void run() {
            future.set(null);
          }
        });
      }
    };
    parent.close().get(1, TimeUnit.SECONDS);
    assertTrue(future.isDone());
  }

  @Test
  public void testCloseQuietly() throws Exception {
    final SettableFuture<?> called = SettableFuture.create();
    final Closeable ioCloseable = new Closeable() {
      @Override
      public void close() throws IOException {
        called.set(null);
      }
    };
    SafeCloseableImpl c = new SafeCloseableImpl() {
      @Override
      protected void performClose() {
        closeQuietly(ioCloseable);
      }
    };
    c.close().get(1, TimeUnit.SECONDS);
    called.get(1, TimeUnit.SECONDS);
  }

  static void listenLogger(org.slf4j.Logger logger, Handler handler) {
    listenLogger(java.util.logging.Logger.getLogger(logger.getName()), handler);
  }

  static void listenLogger(java.util.logging.Logger logger, Handler handler) {
    logger.setLevel(Level.ALL);
    logger.addHandler(handler);
  }

  @Test
  public void testCloseQuietlyException() throws Exception {
    final IOException exception = new IOException("don't swallow me");
    final Closeable ioCloseable = new Closeable() {
      @Override
      public void close() throws IOException {
        throw exception;
      }
    };
    SafeCloseableImpl c = new SafeCloseableImpl() {
      @Override
      protected void performClose() {
        closeQuietly(ioCloseable);
      }
    };
    final SettableFuture<Throwable> thrown = SettableFuture.create();
    listenLogger(c.getLogger(), new Handler() {
      @Override
      public void publish(LogRecord record) {
        Throwable t = record.getThrown();
        if (t != null) {
          thrown.set(t);
        }
      }

      @Override
      public void flush() { }

      @Override
      public void close() throws SecurityException { }
    });
    c.close().get(1, TimeUnit.SECONDS);
    assertEquals(exception, thrown.get(1, TimeUnit.SECONDS));
  }
}

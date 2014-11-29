package djgcv.ssjp.util;

import java.util.concurrent.TimeUnit;

import org.junit.After;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public abstract class SafeCloseableTestBase extends SafeCloseableImpl {
  private final SettableFuture<Throwable> closeCause = SettableFuture.create();

  @Override
  public ListenableFuture<?> close(Throwable cause) {
    closeCause.set(cause);
    return super.close(cause);
  }

  @After
  public void endOfTest() throws Throwable {
    finalTearDown();
    Throwable cause = closeCause.get();
    if (cause != null) {
      throw cause;
    }
  }

  protected void finalTearDown() throws Exception {
    close().get(10, TimeUnit.SECONDS);
  }

  @Override
  protected void performClose() { }
}

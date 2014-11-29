package djgcv.ssjp.util;

import java.util.concurrent.TimeUnit;

import org.junit.After;

public abstract class SafeCloseableTestBase extends SafeCloseableImpl {
  @After
  protected void finalTearDown() throws Exception {
    close().get(10, TimeUnit.SECONDS);
  }

  @Override
  protected void performClose() { }
}

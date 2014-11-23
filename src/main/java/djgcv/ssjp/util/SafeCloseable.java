package djgcv.ssjp.util;

import com.google.common.util.concurrent.ListenableFuture;

public interface SafeCloseable {
  ListenableFuture<?> close(Throwable cause);

  ListenableFuture<?> close();

  boolean isClosing();

  boolean isClosed();

  ListenableFuture<?> getCloseFuture();
}

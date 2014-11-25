package djgcv.ssjp.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public abstract class SafeCloseableImpl implements SafeCloseable {
  static final Logger log = LoggerFactory.getLogger(SafeCloseableImpl.class);

  private final AtomicReference<CompletionCount> taskRef = new AtomicReference<CompletionCount>();

  protected abstract void performClose();

  protected ListeningExecutorService getCloseExecutor() {
    return MoreExecutors.sameThreadExecutor();
  }

  protected Logger getLogger() {
    return log;
  }

  @Override
  public ListenableFuture<?> close(Throwable cause) {
    Logger log = getLogger();
    boolean logged = false;
    CompletionCount task = taskRef.get();
    if (task == null) {
      task = new CompletionCount(1);
      if (taskRef.compareAndSet(null, task)) {
        if (cause != null) {
          log.error("Closing " + this + " due to error", cause);
        } else if (log.isDebugEnabled()) {
          log.debug("Closing " + this);
        }
        logged = true;
        task.addChild(getCloseExecutor().submit(new Runnable() {
          @Override
          public void run() {
            performClose();
          }
        }));
        task.removeTask();
      } else {
        task = taskRef.get();
      }
    }
    if (!logged && cause != null && log.isDebugEnabled()) {
      log.debug("Got extra error for already-closing " + this, cause);
    }
    return task;
  }

  @Override
  public ListenableFuture<?> close() {
    return close(null);
  }

  @Override
  public ListenableFuture<?> getCloseFuture() {
    return taskRef.get();
  }

  @Override
  public boolean isClosing() {
    return getCloseFuture() != null;
  }

  @Override
  public boolean isClosed() {
    ListenableFuture<?> task = getCloseFuture();
    return task != null && task.isDone();
  }

  protected void closeSafeCloseable(SafeCloseable child) {
    addCloseTask(child.close());
  }

  protected void addCloseTask(ListenableFuture<?> childTask) {
    CompletionCount task = taskRef.get();
    if (task != null) {
      task.addChild(childTask);
    }
  }

  protected void runCloseTask(Runnable childTask) {
    addCloseTask(getCloseExecutor().submit(childTask));
  }

  protected void closeQuietly(final java.io.Closeable closeable) {
    runCloseTask(new Runnable() {
      @Override
      public void run() {
        try {
          closeable.close();
        } catch (IOException e) {
          Logger log = getLogger();
          if (log.isDebugEnabled()) {
            log.debug("Error closing " + closeable, e);
          }
        }
      }
    });
  }
}

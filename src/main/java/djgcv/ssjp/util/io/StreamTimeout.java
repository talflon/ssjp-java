package djgcv.ssjp.util.io;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import djgcv.ssjp.util.ActivityTimeout;
import djgcv.ssjp.util.Timeout;

public abstract class StreamTimeout<S extends ActivityCallbackStream>
    implements Timeout {
  private final S stream;
  private ActivityTimeout activityTimeout;

  protected StreamTimeout(S stream) {
    this.stream = stream;
  }

  protected abstract void onTimeout();

  protected abstract ScheduledExecutorService getExecutor();

  public S getStream() { return stream; }

  @Override
  public synchronized void set(long delay, TimeUnit unit) {
    if (activityTimeout == null) {
      activityTimeout = new ActivityTimeout(delay, unit) {
        @Override
        public void run() {
          onTimeout();
        }

        @Override
        public ScheduledExecutorService getExecutor() {
          return StreamTimeout.this.getExecutor();
        }
      };
      activityTimeout.restart();
      getStream().setCallback(new Runnable() {
        @Override
        public void run() {
          restartIfNotStopped();
        }
      });
    } else {
      activityTimeout.set(delay, unit);
    }
  }

  protected synchronized void restartIfNotStopped() {
    if (activityTimeout != null) {
      activityTimeout.restart();
    }
  }

  @Override
  public synchronized void stop() {
    if (activityTimeout != null) {
      getStream().setCallback(null);
      activityTimeout.stop();
      activityTimeout = null;
    }
  }
}

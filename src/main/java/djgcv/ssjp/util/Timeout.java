package djgcv.ssjp.util;

import java.util.concurrent.TimeUnit;

public interface Timeout {
  void stop();

  void set(long delay, TimeUnit unit);
}

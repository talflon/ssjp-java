package djgcv.ssjp.util;

import java.util.Random;

public class ThreadLocalRandomSource extends ThreadLocal<Random>
    implements RandomSource {
  @Override
  public Random getRandom() {
    return get();
  }

  private static final ThreadLocalRandomSource theDefault = new ThreadLocalRandomSource() {
    @Override
    protected Random initialValue() {
      return new Random();
    }
  };

  public static final ThreadLocalRandomSource getDefault() {
    return theDefault;
  }
}

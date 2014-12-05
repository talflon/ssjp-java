package djgcv.ssjp.util;

import java.util.Random;

public class SingleRandomSource implements RandomSource {
  private final Random random;

  public SingleRandomSource(Random random) {
    this.random = random;
  }

  @Override
  public Random getRandom() {
    return random;
  }
}

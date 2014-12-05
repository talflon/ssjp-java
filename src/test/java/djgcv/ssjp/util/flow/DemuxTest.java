package djgcv.ssjp.util.flow;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class DemuxTest {
  private static <K, T> Demux<K, T> createUselessDemux() {
    return new Demux<K, T>() {
      @Override
      protected T muxValue(T value, K key) {
        return value;
      }

      @Override
      protected K getNextKey() {
        return null;
      }

      @Override
      public Receiver<? super T> getInput() {
        return null;
      }
    };
  }

  @Test
  public void testClose() throws Exception {
    Demux<?, ?> demux = createUselessDemux();
    demux.close().get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseChild() throws Exception {
    Demux<?, Object> demux = createUselessDemux();
    demux.connect().close().get(1, TimeUnit.SECONDS);
    demux.close().get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseWithChild() throws Exception {
    Demux<?, Object> demux = createUselessDemux();
    demux.connect();
    demux.close().get(1, TimeUnit.SECONDS);
  }
}

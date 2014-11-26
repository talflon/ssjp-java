package djgcv.ssjp.util.flow;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class DemuxTest {
  private static <K, T> Demux<K, T> createUselessDemux() {
    return new Demux<K, T>() {
      @Override
      public boolean handle(T value) {
        return false;
      }

      @Override
      protected T muxValue(T value, K key) {
        return value;
      }

      @Override
      protected K getNextKey() {
        return null;
      }
    };
  }

  private static <T> Receiver<T> createUselessReceiver() {
    return new Receiver<T>() {
      @Override
      public void receive(T value) { }
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
    demux.connect(createUselessReceiver()).close().get(1, TimeUnit.SECONDS);
    demux.close().get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseWithChild() throws Exception {
    Demux<?, Object> demux = createUselessDemux();
    demux.connect(createUselessReceiver());
    demux.close().get(1, TimeUnit.SECONDS);
  }
}

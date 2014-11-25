package djgcv.ssjp.util.flow;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class DemuxTest {

  private static Demux<?, ?> createUselessDemux() {
    return new Demux<Object, Object>() {
      @Override
      public boolean handle(Object value) {
        return false;
      }

      @Override
      protected Object muxValue(Object value, Object key) {
        return value;
      }

      @Override
      protected Object getNextKey() {
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
    Demux<?, ?> demux = createUselessDemux();
    demux.connect().close().get(1, TimeUnit.SECONDS);
    demux.close().get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testCloseWithChild() throws Exception {
    Demux<?, ?> demux = createUselessDemux();
    demux.connect();
    demux.close().get(1, TimeUnit.SECONDS);
  }
}

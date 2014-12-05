package djgcv.ssjp.util.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterables;

public class HandlerMapTest {

  private HandlerMap<Integer, String> handlerMap;

  @Before
  public void setUp() throws Exception {
    handlerMap = new HandlerMap<Integer, String>() {
      @Override
      protected Integer getKey(String value) {
        try {
          return Integer.valueOf(value);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    };
  }

  static <T> Receiver<T> createDummyHandler(final boolean handles) {
    return new Receiver<T>() {
      @Override
      public boolean receive(T value) {
        return handles;
      }
    };
  }

  @Test
  public void testGetHandlers() {
    for (int key = 1; key < 5; key++) {
      ReceiverList<String> handlers = handlerMap.getHandlers(key);
      assertEquals(0, Iterables.size(handlers.getReceivers()));
      handlers.appendReceiver(createDummyHandler(true));
      assertSame(handlers, handlerMap.getHandlers(key));
    }
  }

  @Test
  public void testHandled() throws Exception {
    for (int key = 1; key < 5; key++) {
      FutureReceiver<String> result = new FutureReceiver<String>();
      handlerMap.getHandlers(key).appendReceiver(result);
      String value = Integer.toString(key);
      assertTrue(handlerMap.receive(value));
      assertEquals(value, result.get(0, TimeUnit.SECONDS));
    }
  }

  @Test
  public void testUnhandled() throws Exception {
    for (int key = 1; key < 5; key++) {
      assertFalse(handlerMap.receive(Integer.toString(key)));
    }
    assertFalse(handlerMap.receive("blah"));
  }
}

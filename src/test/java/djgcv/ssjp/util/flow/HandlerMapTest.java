package djgcv.ssjp.util.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.SettableFuture;

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

  static <T> Handler<T> createDummyHandler(final boolean handles) {
    return new HandlerImpl<T>() {
      @Override
      public boolean handle(T value) {
        return handles;
      }
    };
  }

  @Test
  public void testGetHandlers() {
    for (int key = 1; key < 5; key++) {
      HandlerList<String> handlers = handlerMap.getHandlers(key);
      assertEquals(0, Iterables.size(handlers.getReceivers()));
      handlers.appendReceiver(createDummyHandler(true));
      assertSame(handlers, handlerMap.getHandlers(key));
    }
  }

  @Test
  public void testFindHandlers() {
    for (int key = 1; key < 5; key++) {
      assertNull(handlerMap.findHandlers(key));
      HandlerList<String> handlers = handlerMap.getHandlers(key);
      handlers.appendReceiver(createDummyHandler(true));
      assertSame(handlers, handlerMap.findHandlers(key));
    }
  }

  @Test
  public void testHandled() throws Exception {
    for (int key = 1; key < 5; key++) {
      final SettableFuture<String> result = SettableFuture.create();
      handlerMap.getHandlers(key).appendReceiver(new HandlerImpl<String>() {
        @Override
        public boolean handle(String value) {
          return result.set(value);
        }
      });
      String value = Integer.toString(key);
      assertTrue(handlerMap.handle(value));
      assertEquals(value, result.get(0, TimeUnit.SECONDS));
    }
  }

  @Test
  public void testUnhandled() throws Exception {
    for (int key = 1; key < 5; key++) {
      assertFalse(handlerMap.handle(Integer.toString(key)));
    }
    assertFalse(handlerMap.handle("blah"));
  }
}

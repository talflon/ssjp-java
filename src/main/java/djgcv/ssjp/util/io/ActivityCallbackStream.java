package djgcv.ssjp.util.io;

public interface ActivityCallbackStream {
  Runnable getCallback();

  void setCallback(Runnable callback);
}
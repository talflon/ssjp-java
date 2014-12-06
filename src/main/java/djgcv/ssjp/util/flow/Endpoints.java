package djgcv.ssjp.util.flow;

public final class Endpoints {
  public static <T> Endpoint<T> wrap(final Receiver<? super T> input, final ReceiverList<T> output) {
    return new AbstractEndpoint<T>() {
      @Override
      public Receiver<? super T> getInput() {
        return input;
      }

      @Override
      public ReceiverList<T> getOutput() {
        return output;
      }

      @Override
      protected void performClose() { }
    };
  }

  private Endpoints() { }
}

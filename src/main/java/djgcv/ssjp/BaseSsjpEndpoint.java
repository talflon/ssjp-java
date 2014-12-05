package djgcv.ssjp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import djgcv.ssjp.util.ExecutorShop;
import djgcv.ssjp.util.Timeout;
import djgcv.ssjp.util.flow.ConcurrentPipe;
import djgcv.ssjp.util.flow.EndpointImpl;
import djgcv.ssjp.util.flow.FutureReceiver;
import djgcv.ssjp.util.flow.Pipe;
import djgcv.ssjp.util.flow.Receiver;
import djgcv.ssjp.util.flow.jackson.JsonObjectInputter;
import djgcv.ssjp.util.flow.jackson.JsonObjectOutputter;
import djgcv.ssjp.util.io.ActivityCallbackInputStream;
import djgcv.ssjp.util.io.ActivityCallbackOutputStream;
import djgcv.ssjp.util.io.ActivityCallbackStream;

abstract class BaseSsjpEndpoint extends EndpointImpl<ObjectNode> implements
    SsjpEndpoint {
  static final Logger log = LoggerFactory.getLogger(BaseSsjpEndpoint.class);

  private final ObjectMapper mapper;
  private final ExecutorShop executorShop;
  private final JsonObjectInputter inputter;
  private final JsonObjectOutputter outputter;
  private final ObjectNode ourOptions;
  private final SettableFuture<Receiver<? super ObjectNode>> inputFuture =
      SettableFuture.create();
  private final StreamTimeout<ActivityCallbackInputStream> inputTimeout;
  private final StreamTimeout<ActivityCallbackOutputStream> outputTimeout;

  protected BaseSsjpEndpoint(ObjectMapper mapper, ExecutorShop executorShop,
      InputStream inputStream, OutputStream outputStream, ObjectNode options)
      throws IOException {
    this.mapper = mapper;
    this.executorShop = executorShop;
    ourOptions = options;
    JsonFactory factory = mapper.getFactory();
    inputTimeout = new StreamTimeout<ActivityCallbackInputStream>(
        new ActivityCallbackInputStream(inputStream)) {
      @Override
      protected void onTimeout() {
        onInputTimeout();
      }
    };
    outputTimeout = new StreamTimeout<ActivityCallbackOutputStream>(
        new ActivityCallbackOutputStream(outputStream)) {
      @Override
      protected void onTimeout() {
        onOutputTimeout();
      }
    };
    inputter = new JsonObjectInputter(factory,
        wrapInputStream(inputTimeout.getStream()), true);
    outputter = new JsonObjectOutputter(factory,
        wrapOutputStream(outputTimeout.getStream()), true);
  }

  protected BaseSsjpEndpoint(ObjectMapper mapper, ExecutorShop executorShop,
      Socket socket, ObjectNode options) throws IOException {
    this(mapper, executorShop, socket.getInputStream(), socket.getOutputStream(),
        options);
  }

  public void start() {
    getExecutorShop().getBlockingExecutor().execute(new Runnable() {
      @Override
      public void run() {
        try {
          startHandshake();
        } catch (Exception e) {
          close(e);
        }
      }
    });
  }

  @Override
  public Receiver<? super ObjectNode> getInput() {
    try {
      return getInputFuture().get(0, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public ListenableFuture<Receiver<? super ObjectNode>> getInputFuture() {
    return inputFuture;
  }

  public ExecutorShop getExecutorShop() {
    return executorShop;
  }

  protected InputStream wrapInputStream(InputStream inputStream) {
    return new BufferedInputStream(inputStream);
  }

  protected OutputStream wrapOutputStream(OutputStream outputStream) {
    return new BufferedOutputStream(outputStream);
  }

  protected void onInputTimeout() {
    log.info("Closing due to idle input");
    close();
  }

  protected void onOutputTimeout() {
    log.debug("Sending keep-alive");
    getInput().receive(JsonObjectOutputter.KEEPALIVE);
  }

  private abstract class StreamTimeout<S extends ActivityCallbackStream>
      extends djgcv.ssjp.util.io.StreamTimeout<S> {
    protected StreamTimeout(S stream) {
      super(stream);
    }

    @Override
    protected ScheduledExecutorService getExecutor() {
      return getExecutorShop().getScheduler();
    }
  }

  @Override
  public Timeout getInputTimeout() {
    return inputTimeout;
  }

  @Override
  public Timeout getOutputTimeout() {
    return outputTimeout;
  }

  protected abstract void startHandshake();

  protected abstract void afterSending();

  protected abstract void afterReceiving();

  protected void sendOurGreeting() {
    log.debug("Sending greeting");
    outputter.receive(Handshaking.createGreeting(mapper, ourOptions));
    afterSending();
  }

  protected void checkTheirGreeting(ObjectNode greeting) throws Exception {
    log.debug("Checking received greeting: " + greeting);
    JsonNode version = greeting.get("ssjp");
    if (!(version != null && version.isTextual() && Handshaking.SSJP_VERSION
        .equals(version.asText()))) {
      throw new Exception("Bad version: " + version); // TODO type?
    }
  }

  protected ListenableFuture<ObjectNode> getTheirGreeting() {
    final FutureReceiver<ObjectNode> result = new FutureReceiver<ObjectNode>();
    inputter.getReceiverList().appendReceiver(result);
    Futures.addCallback(result, new FutureCallback<ObjectNode>() {
      @Override
      public void onFailure(Throwable thrown) {
        close(thrown);
      }

      @Override
      public void onSuccess(ObjectNode greeting) {
        try {
          inputter.getReceiverList().removeReceiver(result);
          checkTheirGreeting(greeting);
          afterReceiving();
        } catch (Exception e) {
          close(e);
        }
      }
    });
    getExecutorShop().getBlockingExecutor().execute(new Runnable() {
      @Override
      public void run() {
        try {
          inputter.inputOneValue();
        } catch (IOException e) {
          close(e);
        }
      }
    });
    return result;
  }

  protected void finishHandshaking() {
    log.debug("Finished handshaking; creating output pipe");
    Pipe<ObjectNode> pipe = new ConcurrentPipe<ObjectNode>(
        getExecutorShop().getBlockingExecutor());
    pipe.getOutput().appendReceiver(outputter);
    inputFuture.set(pipe.getInput());
    log.debug("Spawning input loop");
    inputter.getReceiverList().appendReceiver(getOutputPipe().getInput());
    getExecutorShop().getBlockingExecutor().execute(new Runnable() {
      @Override
      public void run() {
        try {
          while (true) {
            inputter.inputOneValue();
          }
        } catch (Exception e) {
          log.debug("Stopped inputting due to exception", e);
          close(e);
        }
      }
    });
  }

  @Override
  public ListenableFuture<?> close(Throwable cause) {
    inputFuture.setException((cause != null) ? cause
        : new IllegalStateException("close() called"));
    return super.close(cause);
  }

  @Override
  protected void performClose() {
    cleanupSafeCloseable(inputter);
    cleanupSafeCloseable(outputter);
    getCloseFuture().addListener(new Runnable() {
      @Override
      public void run() {
        getInputTimeout().stop();
        getOutputTimeout().stop();
      }
    }, MoreExecutors.sameThreadExecutor());
  }
}

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

import djgcv.ssjp.util.ActivityTimeout;
import djgcv.ssjp.util.flow.ConcurrentPipe;
import djgcv.ssjp.util.flow.EndpointImpl;
import djgcv.ssjp.util.flow.FutureHandler;
import djgcv.ssjp.util.flow.Pipe;
import djgcv.ssjp.util.flow.Receiver;
import djgcv.ssjp.util.flow.jackson.JsonObjectInputter;
import djgcv.ssjp.util.flow.jackson.JsonObjectOutputter;
import djgcv.ssjp.util.io.ActivityCallbackInputStream;
import djgcv.ssjp.util.io.ActivityCallbackOutputStream;

abstract class BaseSsjpEndpoint extends EndpointImpl<ObjectNode> implements
    SsjpEndpoint {
  static final Logger log = LoggerFactory.getLogger(BaseSsjpEndpoint.class);

  private final ObjectMapper mapper;
  private final ScheduledExecutorService executor;
  private final JsonObjectInputter inputter;
  private final JsonObjectOutputter outputter;
  private final ObjectNode ourOptions;
  private final ActivityCallbackInputStream callbackInputStream;
  private final ActivityCallbackOutputStream callbackOutputStream;
  private final SettableFuture<Receiver<? super ObjectNode>> inputFuture =
      SettableFuture.create();
  private ActivityTimeout inputTimeout, outputTimeout;

  protected BaseSsjpEndpoint(ObjectMapper mapper,
      ScheduledExecutorService executor,
      InputStream inputStream, OutputStream outputStream, ObjectNode options)
      throws IOException {
    this.mapper = mapper;
    this.executor = executor;
    ourOptions = options;
    JsonFactory factory = mapper.getFactory();
    callbackInputStream = new ActivityCallbackInputStream(inputStream);
    callbackOutputStream = new ActivityCallbackOutputStream(outputStream);
    inputter = new JsonObjectInputter(factory,
        wrapInputStream(callbackInputStream), true);
    outputter = new JsonObjectOutputter(factory,
        wrapOutputStream(callbackOutputStream), true);
  }

  protected BaseSsjpEndpoint(ObjectMapper mapper,
      ScheduledExecutorService executor,
      Socket socket, ObjectNode options) throws IOException {
    this(mapper, executor, socket.getInputStream(), socket.getOutputStream(),
        options);
  }

  public void start() {
    executor.execute(new Runnable() {
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

  protected InputStream wrapInputStream(InputStream inputStream) {
    return new BufferedInputStream(inputStream);
  }

  protected OutputStream wrapOutputStream(OutputStream outputStream) {
    return new BufferedOutputStream(outputStream);
  }

  protected ActivityCallbackInputStream getCallbackInputStream() {
    return callbackInputStream;
  }

  protected ActivityCallbackOutputStream getCallbackOutputStream() {
    return callbackOutputStream;
  }

  public void setInputTimeout(long delay, TimeUnit unit) {
    synchronized (this) {
      if (inputTimeout == null) {
        inputTimeout = new ActivityTimeout(delay, unit) {
          @Override
          public void run() {
            onInputTimeout();
          }

          @Override
          public ScheduledExecutorService getExecutor() {
            return executor;
          }
        };
        inputTimeout.restart();
        getCallbackInputStream().setCallback(new Runnable() {
          @Override
          public void run() {
            inputTimeout.restart();
          }
        });
      } else {
        inputTimeout.setTimeout(delay, unit);
      }
    }
  }

  public void setOutputTimeout(long delay, TimeUnit unit) {
    synchronized (this) {
      if (outputTimeout == null) {
        outputTimeout = new ActivityTimeout(delay, unit) {
          @Override
          public void run() {
            onOutputTimeout();
          }

          @Override
          public ScheduledExecutorService getExecutor() {
            return executor;
          }
        };
        outputTimeout.restart();
        getCallbackOutputStream().setCallback(new Runnable() {
          @Override
          public void run() {
            outputTimeout.restart();
          }
        });
      } else {
        outputTimeout.setTimeout(delay, unit);
      }
    }
  }

  protected void onInputTimeout() {
    log.info("Closing due to idle input");
    close();
  }

  protected void onOutputTimeout() {
    log.debug("Sending keep-alive");
    getInput().receive(JsonObjectOutputter.KEEPALIVE);
  }

  protected abstract void startHandshake();

  protected abstract void afterSending();

  protected abstract void afterReceiving();

  protected void sendOurGreeting() {
    log.debug("Sending greeting");
    outputter.handle(Handshaking.createGreeting(mapper, ourOptions));
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
    final FutureHandler<ObjectNode> result = new FutureHandler<ObjectNode>();
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
    executor.execute(new Runnable() {
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
    Pipe<ObjectNode> pipe = new ConcurrentPipe<ObjectNode>(executor);
    pipe.getOutput().appendReceiver(outputter);
    inputFuture.set(pipe.getInput());
    log.debug("Spawning input loop");
    inputter.getReceiverList().appendReceiver(getOutputPipe().getInput());
    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          while (true) { // XXX terrible?
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
    getCallbackInputStream().setCallback(null);
    getCallbackOutputStream().setCallback(null);
    if (inputter != null) {
      closeSafeCloseable(inputter);
    }
    if (outputter != null) {
      closeSafeCloseable(outputter);
    }
    getCloseFuture().addListener(new Runnable() {
      @Override
      public void run() {
        inputTimeout.stop();
        outputTimeout.stop();
      }
    }, MoreExecutors.sameThreadExecutor());
  }
}

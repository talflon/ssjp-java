package djgcv.ssjp.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import djgcv.ssjp.HandlerPathMap;
import djgcv.ssjp.HandlerRequestMap;
import djgcv.ssjp.MessageIdDemux;
import djgcv.ssjp.Messages;
import djgcv.ssjp.SsjpClientEndpoint;
import djgcv.ssjp.util.ExecutorShop;
import djgcv.ssjp.util.ExecutorShops;
import djgcv.ssjp.util.flow.ConcurrentPipe;
import djgcv.ssjp.util.flow.HandlerPipeImpl;
import djgcv.ssjp.util.flow.Handlers;
import djgcv.ssjp.util.flow.Nodes;
import djgcv.ssjp.util.flow.Pipe;
import djgcv.ssjp.util.flow.Receiver;

public class ChatClientGui extends JFrame {
  protected static final ObjectMapper mapper = new ObjectMapper();

  protected final ExecutorShop executorShop;
  protected final JTextArea outputText;
  protected final JTextField inputField;
  private final Receiver<ObjectNode> inputHandler;
  private final MessageIdDemux demux = new MessageIdDemux();
  private SsjpClientEndpoint client;

  public ChatClientGui(ExecutorShop executorShop) {
    super("SSJP Example Chat Client");
    this.executorShop = executorShop;

    outputText = new JTextArea();
    outputText.setEditable(false);
    add(new JScrollPane(outputText), BorderLayout.CENTER);
    Box bottomBox = Box.createHorizontalBox();
    inputField = new JTextField();
    inputField.addActionListener(processAction);
    bottomBox.add(inputField);
    bottomBox.add(new JButton(processAction));
    add(bottomBox, BorderLayout.SOUTH);

    HandlerRequestMap reqMap = new HandlerRequestMap();
    reqMap.getHandlers("said").appendReceiver(handleSaid);
    HandlerPathMap pathMap = new HandlerPathMap();
    pathMap.getHandlers(ChatServer.PATH).appendReceiver(reqMap);
    Pipe<ObjectNode> inputPipe = new HandlerPipeImpl<ObjectNode>();
    inputPipe.getOutput().appendReceiver(pathMap);
    inputPipe.getOutput().appendReceiver(
        Handlers.forReceiver(demux.getInput(), true));
    inputPipe.getOutput().appendReceiver(new Receiver<ObjectNode>() {
      @Override
      public boolean receive(ObjectNode value) {
        addOutput("*** UNEXPECTED MESSAGE: " + value);
        return true;
      }
    });
    Pipe<ObjectNode> cPipe = new ConcurrentPipe<ObjectNode>(
        executorShop.getExecutor());
    cPipe.getOutput().appendReceiver(inputPipe.getInput());
    inputHandler = Handlers.forReceiver(cPipe.getInput(), true);
  }

  static final Pattern CONNECT_PATTERN =
      Pattern.compile("^(\\w+)?(?::(\\d{1,5}))?$");

  protected final Receiver<ObjectNode> handleSaid = new Receiver<ObjectNode>() {
    @Override
    public boolean receive(ObjectNode msg) {
      JsonNode args = msg.path("arg");
      JsonNode who = args.path("who");
      JsonNode what = args.path("what");
      if (who.isTextual() && what.isTextual()) {
        addOutput(who.asText() + ": " + what.asText());
        return true;
      } else {
        return false;
      }
    }
  };

  protected final Action processAction = new AbstractAction("OK") {
    @Override
    public void actionPerformed(ActionEvent e) {
      String command = inputField.getText();
      inputField.setText("");
      addOutput("> " + command);
      if (command.charAt(0) == '\\') {
        String[] split = command.split("\\s+");
        if ("\\nick".equals(split[0])) {
          if (split.length == 2) {
            requestNick(split[1]);
          } else {
            addOutput("*** TOO MANY ARGUMENTS");
          }
        } else if ("\\enter".equals(split[0])) {
          if (split.length == 2) {
            Matcher m = CONNECT_PATTERN.matcher(split[1]);
            if (m.matches()) {
              String host = m.group(1);
              if ("".equals(host)) {
                host = "localhost";
              }
              int port;
              if (m.group(2).length() > 0) {
                port = Integer.parseInt(m.group(2));
              } else {
                port = ChatServer.PORT;
              }
              connectTo(host, port);
            } else {
              addOutput("*** INVALID ARGUMENT");
            }
          } else {
            addOutput("*** TOO MANY ARGUMENTS");
          }
        } else if ("\\exit".equals(split[0])) {
          closeConnection();
        } else {
          addOutput("*** UNKNOWN COMMAND: " + split[0]);
        }
      } else {
        send(command);
      }
    }
  };

  protected void send(String what) {
    if (getConnectedClientOrReport() == null) {
      return;
    }
    client.getInput().receive(
        Messages.request(mapper, ChatServer.PATH, "say",
            mapper.createObjectNode().put("what", what)));
  }

  protected void requestNick(final String nick) {
    if (getConnectedClientOrReport() == null) {
      return;
    }
    Futures.addCallback(
        Nodes.sendRequest(demux,
            Messages.request(mapper, ChatServer.PATH, "nick",
                mapper.createObjectNode().put("nick", nick))),
        new FutureCallback<ObjectNode>() {
          @Override
          public void onFailure(Throwable cause) { }

          @Override
          public void onSuccess(ObjectNode msg) {
            JsonNode success = msg.path("rsp").path("success");
            if (success.isBoolean() && success.asBoolean()) {
              addOutput("You are now known as \"" + nick + "\"");
            }
          }
        });
  }

  protected void closeConnection() {
    SsjpClientEndpoint client = getConnectedClientOrReport();
    if (client != null && !client.isClosing()) {
      client.close().addListener(new Runnable() {
        @Override
        public void run() {
          addOutput("*** CONNECTION CLOSED");
        }
      }, MoreExecutors.sameThreadExecutor());
    }
  }

  protected void addOutput(String text) {
    final String toAppend = text + '\n';
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        outputText.append(toAppend);
      }
    });
  }

  protected SsjpClientEndpoint getConnectedClientOrReport() {
    SsjpClientEndpoint client;
    synchronized (this) {
      client = this.client;
    }
    try {
      if (client != null) {
        client.getInput();
        if (!client.isClosing()) {
          return client;
        }
      }
    } catch (IllegalStateException e) {
      /* fall through */
    }
    addOutput("*** NOT CONNECTED");
    return null;
  }

  protected synchronized void closeClient() {
    if (client != null) {
      demux.getOutput().removeReceiver(Handlers.forReceiver(client.getInput(), true));
      client.close();
      client = null;
    }
  }

  protected synchronized void setClient(SsjpClientEndpoint client) {
    closeClient();
    this.client = client;
  }

  protected void connectTo(final String host, final int port) {
    try {
      Socket socket = new Socket(host, port);
      final SsjpClientEndpoint client = new SsjpClientEndpoint(
          new ObjectMapper(), executorShop, socket, null);
      client.getOutput().appendReceiver(inputHandler);
      client.start();
      setClient(client);
      client.getInputFuture().addListener(new Runnable() {
        @Override
        public void run() {
          demux.getOutput().appendReceiver(Handlers.forReceiver(client.getInput(), true));
          addOutput("*** CONNECTED to " + host + ":" + port);
        }
      }, MoreExecutors.sameThreadExecutor());
    } catch (Exception e) {
      final String type = e.toString();
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          addOutput("*** ERROR CONNECTING: " + type);
        }
      });
    }
  }

  public static void main(String[] args) throws Exception {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        ChatClientGui frame = new ChatClientGui(ExecutorShops.create());
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setVisible(true);
      }
    });
  }
}

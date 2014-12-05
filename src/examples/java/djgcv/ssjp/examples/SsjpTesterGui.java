package djgcv.ssjp.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import com.google.common.html.HtmlEscapers;
import com.google.common.util.concurrent.MoreExecutors;

import djgcv.ssjp.Messages;
import djgcv.ssjp.SsjpClientEndpoint;
import djgcv.ssjp.util.ExecutorShop;
import djgcv.ssjp.util.ExecutorShops;
import djgcv.ssjp.util.flow.Receiver;

public class SsjpTesterGui extends JFrame {
  protected final ExecutorShop executorShop;
  protected final JTextArea inputText;
  protected final JEditorPane outputText;
  private final HTMLDocument outputDoc;
  private final Element outputElement;
  protected final JTextField connectField;
  private SsjpClientEndpoint client;

  public SsjpTesterGui(ExecutorShop executorShop) {
    super("SSJP Tester");
    this.executorShop = executorShop;

    outputText = new JEditorPane("text/html",
        "<html><body><div id=\"output\"></div></body></html>");
    outputText.setEditable(false);
    outputDoc = (HTMLDocument) outputText.getDocument();
    outputElement = outputDoc.getElement("output");

    JPanel controlPanel = new JPanel(new BorderLayout());
    {
      Box connectBox = Box.createHorizontalBox();
      {
        connectField = new JTextField();
        connectBox.add(connectField);
        connectBox.add(new JButton(connectAction));
        controlPanel.add(connectBox, BorderLayout.NORTH);
      }
      inputText = new JTextArea();
      inputText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
      controlPanel.add(new JScrollPane(inputText), BorderLayout.CENTER);
      Box controlBox = Box.createHorizontalBox();
      {
        controlBox.add(new JButton(clearAction));
        controlBox.add(Box.createGlue());
        controlBox.add(new JButton(newRequestAction));
        controlBox.add(Box.createGlue());
        controlBox.add(new JButton(newResponseAction));
        controlBox.add(Box.createGlue());
        controlBox.add(new JButton(reformatAction));
        controlBox.add(Box.createGlue());
        controlBox.add(new JButton(sendAction));
        controlBox.setBackground(Color.RED);
        controlPanel.add(controlBox, BorderLayout.SOUTH);
      }
    }

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        new JScrollPane(outputText), controlPanel);
    add(splitPane);
    splitPane.setDividerLocation(0.5);
    splitPane.setResizeWeight(0.5);

    sendAction.setEnabled(false);
    updateConnectionStatus();
  }

  protected synchronized Optional<SsjpClientEndpoint> getClient() {
    if (client != null && client.isClosing()) {
      client = null;
    }
    return Optional.fromNullable(client);
  }

  protected synchronized void closeClient() {
    if (client != null) {
      client.close();
      client = null;
    }
  }

  protected synchronized void setClient(SsjpClientEndpoint client) {
    closeClient();
    this.client = client;
  }

  protected boolean isConnected() {
    Optional<SsjpClientEndpoint> client = getClient();
    return client.isPresent()
        && client.get().getInputFuture().isDone()
        && !client.get().isClosing();
  }

  protected void connectTo(String host, int port) {
    try {
      Socket socket = new Socket(host, port);
      final SsjpClientEndpoint client = new SsjpClientEndpoint(
          new ObjectMapper(), executorShop, socket, null);
      client.start();
      setClient(client);
      client.getInputFuture().addListener(new Runnable() {
        @Override
        public void run() {
          updateConnectionStatus();
        }
      }, new Executor() { // TODO define elsewhere
        @Override
        public void execute(Runnable command) {
          EventQueue.invokeLater(command);
        }
      });
      client.getInputFuture().addListener(new Runnable() {
        @Override
        public void run() {
          client.getOutput().prependReceiver(new Receiver<ObjectNode>() {
            @Override
            public boolean receive(ObjectNode value) {
              ObjectMapper mapper = new ObjectMapper();
              mapper.enable(SerializationFeature.INDENT_OUTPUT);
              final String blah;
              try {
                blah = "<p>Received:<pre>" + HtmlEscapers.htmlEscaper().escape(
                    mapper.writeValueAsString(value)) + "</pre></p>";
              } catch (JsonProcessingException e) {
                throw new Error(e);
              }
              EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                  addOutput(blah);
                }
              });
              return true;
            }
          });
        }
      }, MoreExecutors.sameThreadExecutor());
    } catch (Exception e) {
      final String type = e.toString();
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          JOptionPane.showMessageDialog(SsjpTesterGui.this,
              type,
              "Error connecting",
              JOptionPane.ERROR_MESSAGE);
        }
      });
    }
  }

  protected void send(ObjectNode message) {
    client.getInput().receive(message);
  }

  protected void updateConnectionStatus() {
    boolean connected = isConnected();
    if (sendAction.isEnabled() != connected) {
      if (connected) {
        addOutput("<p>Connected</p>");
      } else {
        addOutput("<p>Disconnected</p>");
      }
      sendAction.setEnabled(connected);
    }
  }

  protected void addOutput(String html) {
    try {
      outputDoc.insertBeforeEnd(outputElement, html);
    } catch (BadLocationException e) {
      throw new Error(e);
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  static final Pattern CONNECT_PATTERN =
      Pattern.compile("^(\\w+):(\\d{1,5})$");

  protected final Action connectAction = new AbstractAction("Connect") {
    @Override
    public void actionPerformed(ActionEvent event) {
      Matcher m = CONNECT_PATTERN.matcher(connectField.getText());
      if (!m.matches()) {
        JOptionPane.showMessageDialog(SsjpTesterGui.this,
            "Invalid host or port. Specify as host:port.",
            "Error connecting",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      final String host = m.group(1);
      final int port = Integer.parseInt(m.group(2));
      executorShop.getBlockingExecutor().execute(new Runnable() {
        @Override
        public void run() {
          connectTo(host, port);
        }
      });
    }
  };

  protected final Action clearAction = new AbstractAction("Clear") {
    @Override
    public void actionPerformed(ActionEvent event) {
      inputText.setText("");
    }
  };

  protected final Action newRequestAction = new AbstractAction("New Request") {
    @Override
    public void actionPerformed(ActionEvent event) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      try {
        inputText.setText(mapper.writeValueAsString(
            Messages.request(mapper, "", "",
                mapper.createObjectNode(), mapper.createObjectNode())));
      } catch (JsonProcessingException e) {
        throw new Error(e);
      }
    }
  };

  protected final Action newResponseAction = new AbstractAction("New Response") {
    @Override
    public void actionPerformed(ActionEvent event) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      try {
        inputText.setText(mapper.writeValueAsString(
            Messages.response(mapper,
                mapper.createObjectNode(), mapper.createObjectNode())));
      } catch (JsonProcessingException e) {
        throw new Error(e);
      }
    }
  };

  protected final Action reformatAction = new AbstractAction("Format") {
    @Override
    public void actionPerformed(ActionEvent event) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      try {
        inputText.setText(mapper.writeValueAsString(
            mapper.readTree(inputText.getText())));
      } catch (JsonProcessingException e) {
        notifyBadJson("formatting");
      } catch (IOException e) {
        throw new Error(e);
      }
    }
  };

  protected void notifyBadJson(String doingWhat) {
    JOptionPane.showMessageDialog(this,
        "Invalid JSON text. Correct before " + doingWhat + ".",
        "Error " + doingWhat,
        JOptionPane.ERROR_MESSAGE);
  }

  protected final Action sendAction = new AbstractAction("Send") {
    @Override
    public void actionPerformed(ActionEvent event) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      String origText = inputText.getText();
      try {
        JsonNode json = mapper.readTree(origText);
        if (json.isObject()) {
          String pretty = mapper.writeValueAsString(json);
          addOutput("<p>Sent:<pre>" + pretty + "</pre></p>");
          send((ObjectNode) json);
        }
      } catch (JsonProcessingException e) {
        notifyBadJson("sending");
      } catch (IOException e) {
        throw new Error(e);
      }
    }
  };

  public static void main(String[] args) throws Exception {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        SsjpTesterGui frame = new SsjpTesterGui(ExecutorShops.create());
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setVisible(true);
      }
    });
  }
}

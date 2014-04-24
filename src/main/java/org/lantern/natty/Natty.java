package org.lantern.natty;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Natty {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final int SENDFILESERVER_PORT = 8585; 
    private static final int GTALK_PORT = 5222;
    private static final int NATTY_ALIVE_INTERVAL = 50000;
    private static final String GTALK_HOST = "talk.google.com";
    private static final String DEFAULT_SEND_FILE = "pom.xml";

    /* XMPP server settings */
    private final ConnectionConfiguration config;
    private final Connection gtalk;

    /* natty process settings */
    private ProcessBuilder builder;
    private Process process;

    private String target;
    private boolean sentAnswer;
    /* last XMPP packet received peer ID */
    private String from;

    private ArrayList<String> candidates;
    private BufferedReader reader;
    private BufferedWriter writer;

    private Socket socket;
    public SendFileServer fileServer;


    public Natty() {
      config = new ConnectionConfiguration(GTALK_HOST, GTALK_PORT, "gmail.com");
      gtalk  = new XMPPConnection(config);
      socket = null;
      sentAnswer = false;
      builder = null;
      target = "";
      candidates = new ArrayList<String>();
    }

    /**
     * @param args user name, password, and target
     * @throws XMPPException 
     * @throws InterruptedException 
     * @throws IOException 
     */
  
    public void connectGTalk(String user, String pass) throws XMPPException, 

        InterruptedException, IOException {

      gtalk.connect();
      gtalk.addPacketListener(new PacketListener() {

        @Override
        public void processPacket(final Packet pack) {
          System.out.println("PACKET FROM: "+pack.getFrom());
          if (from == null) {
            from = pack.getFrom();
          }
          final Message msg = (Message) pack;
          handleXMPPMessage(msg, pack.getFrom());
        }
      }, new PacketFilter() {

        @Override
        public boolean accept(final Packet pack) {
          return pack instanceof Message;
        }
      });

      System.out.println("CONNNECTED?!?");

      gtalk.login(user, pass, "natty");

      final Roster roster = gtalk.getRoster();
      roster.addRosterListener(new RosterListener() {

        @Override
        public void presenceChanged(final Presence pres) {
          if (pres.getFrom().contains("natty") && target.equals("offer")) {
            try { 
              from = pres.getFrom();
              createOffer();
            } catch (Exception e) {
              System.out.println("ERROR " + e);
            }
          }
        }

      @Override
        public void entriesUpdated(Collection<String> arg0) {
        }

      @Override
        public void entriesDeleted(Collection<String> arg0) {
        }

      @Override
        public void entriesAdded(Collection<String> arg0) {
        }
      });

      final Collection<RosterEntry> entries = roster.getEntries();
      for (final RosterEntry re : entries) {
        System.out.println(re.getUser());
      }
    }

    private void handleXMPPMessage(final Message msg, String from) {
      try { 
        String body = msg.getBody();
        if (body == null) {
          return;
        }

        NattyMessage nm = NattyMessage.fromJson(body);
        if (nm.isOffer() || nm.isAnswer()) {
          System.out.println("GOT " + nm.getType() + " " + body);

          /* natty process hasn't started yet if we are the answerer */
          if (builder == null) {
            start("");
            sentAnswer = true;
          }

          sendNattyProcess(body);
        }
        else if (nm.isCandidate()) {
          String candidate = nm.getCandidate();
          //candidates.add(body);
          sendNattyProcess(body);
          System.out.println("GOT CANDIDATE: " + body);
        }
      }
      catch (IOException ioe) {

      }
      catch (InterruptedException ie) {
        ie.printStackTrace();
        System.out.println("ERROR " + ie);
      }
    }

    private URI checkAddress(String address) throws Exception {
      URI uri = new URI("natty://" + address);
      String host = uri.getHost();
      int port = uri.getPort();
      if (host == null || port == -1) {
        throw new URISyntaxException(uri.toString(), "Invalid host:port");
      }
      return uri;
    }

    /* should combine this with part of bindPort to remove redundant code */
    private void sendFile(String local, String remote) throws Exception {
        URI localAddress = checkAddress(local);
        URI remoteAddress = checkAddress(remote);
        File file = new File(DEFAULT_SEND_FILE);
        SendFileClient sfc = new SendFileClient(localAddress, remoteAddress,
                file);
        sfc.run();
    }

    /*
    private void bindPort(String address, boolean startFileServer) throws Exception {
      URI uri = checkAddress(address);
      String host = uri.getHost();
      int port = uri.getPort();

      if (startFileServer) {
        System.out.println("Starting file server on port " + port);
        fileServer = new SendFileServer(port);
        fileServer.run();
      } else {
        socket = new Socket();
        socket.bind(new InetSocketAddress(host, port));
        System.out.println("Successfully bound to local port " + port);
      }
    }
    */

    private class Read extends Thread {

      private BufferedReader reader;

        public Read(BufferedReader reader) {
            this.reader = reader;
        }

        public void run() {
            try {
                String line;
                while ((line = this.reader.readLine()) != null) {
                    System.out.println("NATTY RESPONSE " + line);

                    // skip empty lines
                    if (line.isEmpty()) {
                        continue;
                    }

                    try {
                        NattyMessage msg = NattyMessage.fromJson(line);

                        if (msg == null) {
                            continue;
                        }

                        if (msg.is5Tuple()) {
                            // found our 5-tuple!
                            // if answerer, start file server
                            // bindPort(msg.getLocal(), sentAnswer);
                            if (!sentAnswer) {
                                log.debug("Sending file!!");
                                // if we sent the offer, send the file!
                                sendFile(msg.getLocal(), msg.getRemote());
                            } else {
                                System.out.println("Starting file server on "
                                        + checkAddress(msg.getLocal()));
                                fileServer = new SendFileServer(
                                        checkAddress(msg.getLocal()));
                                fileServer.run();
                            }
                            break;
                        }

                        final Message packet = new Message();
                        packet.setTo(from);
                        packet.setBody(line);
                        gtalk.sendPacket(packet);
                    } catch (Exception jse) {
                        continue;
                    }
                }
            } catch (IOException e) {

            }
        }
    }

    /* Run natty process to generate offer */
    private void createOffer() throws IOException, InterruptedException {
        System.out.println("Sending offer to peer " + from);
        start("-offer");
    }

    private void sendNattyProcess(String msg) throws InterruptedException, IOException {
      writer.write(msg);
      writer.newLine();
      writer.flush();
    }

    private void start(final String target) throws IOException, InterruptedException, IOException {

        final List<String> command = new ArrayList<String>();
        command.add("./natty");
        command.add("-debug");
        command.add(target);
        builder = new ProcessBuilder(command);

        builder.redirectErrorStream(true);

        Map<String, String> environ = builder.environment();

        process = builder.start();

        /* initiate reader */
        final InputStream is = process.getInputStream();
        final InputStreamReader isr = new InputStreamReader(is);
        this.reader = new BufferedReader(isr);

        final OutputStream stdin = process.getOutputStream (); 
        this.writer = new BufferedWriter(new OutputStreamWriter(stdin));

        new Read(this.reader).start();
    }

    private void stop() throws IOException {
      process.destroy();
      writer.close();
    }

    public void setTarget(String target) {
      this.target = target;
    }

    public static void main(final String[] args) throws Exception {
      Natty natty = new Natty();
      final String user = args[0];
      final String pass = args[1];
      if (args.length > 2) {
        natty.setTarget(args[2]);
      }

      // Create a connection to the jabber.org server on a specific port.
      natty.connectGTalk(user, pass);
      
      Thread.sleep(NATTY_ALIVE_INTERVAL);

      natty.stop();

    }

}

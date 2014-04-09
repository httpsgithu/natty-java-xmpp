package org.lantern.natty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
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

public class Natty {

    private final ConnectionConfiguration config;
    private final Connection gtalk;

    private ProcessBuilder builder;
    private Process process;
    private String offer;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean sendOffer;

    public Natty() {
      config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
      gtalk  = new XMPPConnection(config);
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
          final Message msg = (Message) pack;
          System.out.println(msg.getBody());
          try { 
            start("");
            sendAnswer();
          }
          catch (IOException ioe) {
            System.out.println("ERROR " + ioe);
          }
          catch (InterruptedException ie) {
            System.out.println("ERROR " + ie);
          }
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
          final String from = pres.getFrom();
          //System.out.println("GOT PRESENCE: "+from);
          if (from.contains("natty")) {
            final Message msg = new Message();
            msg.setTo(from);
            //System.out.println("Sending message: " + message);
            msg.setBody(offer);
            gtalk.sendPacket(msg);
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

    private class Read extends Thread {

      private BufferedReader reader;

      public Read(BufferedReader reader) {
        this.reader = reader;
      }

      public void run() {
        try {
          String line;
          while ((line = this.reader.readLine()) != null) {
            System.out.println(line);
            if (line.contains("offer")) {
              offer = line;
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    private void sendAnswer() throws IOException, InterruptedException {

      System.out.println("Sending reply " + offer);
      writer.write(offer);
      writer.newLine();
      writer.flush();
      writer.close();
      Thread.sleep(4000);
    }

    private void start(final String target) throws IOException, InterruptedException, IOException {

        final List<String> command = new ArrayList<String>();
        command.add("./natty");
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

    private void run() throws IOException, InterruptedException, IOException {
      start("-offer");
      Thread.sleep(30000);
    }

    public static void main(final String[] args) throws Exception {
      // Create a connection to the jabber.org server on a specific port.
      Natty natty = new Natty();
      final String user = args[0];
      final String pass = args[1];

      natty.connectGTalk(user, pass);
      natty.run();
    }

}

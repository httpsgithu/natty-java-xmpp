package org.lantern.natty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    /**
     * @param args user name, password, and target
     * @throws XMPPException 
     * @throws InterruptedException 
     * @throws IOException 
     */
    public static void main(final String[] args) throws XMPPException, 
        InterruptedException, IOException {
        // Create a connection to the jabber.org server on a specific port.
        final ConnectionConfiguration config = 
                new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        
        final Connection gtalk = new XMPPConnection(config);
        gtalk.connect();
        gtalk.addPacketListener(new PacketListener() {
            
            @Override
            public void processPacket(final Packet pack) {
                System.out.println("PACKET FROM: "+pack.getFrom());
                final Message msg = (Message) pack;
                System.out.println(msg.getBody());
            }
        }, new PacketFilter() {
            
            @Override
            public boolean accept(final Packet pack) {
                return pack instanceof Message;
            }
        });
        
        System.out.println("CONNNECTED?!?");
        
        final String user = args[0];
        final String pass = args[1];
        gtalk.login(user, pass, "natty");
        
        final Roster roster = gtalk.getRoster();
        roster.addRosterListener(new RosterListener() {
            
            @Override
            public void presenceChanged(final Presence pres) {
                final String from = pres.getFrom();
                System.out.println("GOT PRESENCE: "+from);
                if (from.contains("natty")) {
                    final Message msg = new Message();
                    msg.setTo(from);
                    msg.setBody("OFFERS AND ANSWERS GO HERE");
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
        
        final String target = args[2];
        runNatty(target);
        Thread.sleep(20000);
    }

    private static void runNatty(final String target) throws IOException {
        final List<String> command = new ArrayList<String>();
        command.add("./natty");
        command.add("-offer");


        final ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environ = builder.environment();

        final Process process = builder.start();
        final InputStream is = process.getInputStream();
        final InputStreamReader isr = new InputStreamReader(is);
        final BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
          System.out.println(line);
        }
        System.out.println("Program terminated!");
    }

}

package org.lantern.natty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

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
        
        System.out.println("CONNNECTED?!?");
        
        final String user = args[0];
        final String pass = args[1];
        gtalk.login(user, pass);
        
        final String target = args[2];
        runNatty(target);
        Thread.sleep(20000);
    }

    private static void runNatty(final String target) throws IOException {
        final List<String> command = new ArrayList<String>();
        command.add("./natty");
        command.add(target);


        final ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environ = builder.environment();

        final Process process = builder.start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
          System.out.println(line);
        }
        System.out.println("Program terminated!");
    }

}

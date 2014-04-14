package org.lantern.natty;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class SendFileTest {

    public static void main(String[] args) throws Exception {
        startNettyServer();
        Thread.sleep(300);
        final File file = new File("pom.xml");
        
        final URI local = new URI("natty://127.0.0.1:7776");
        final URI remote = new URI("natty://127.0.0.1:7777");
        final SendFileClient client = new SendFileClient(local, remote, file);
        client.run();
    }

    private static void startNettyServer() throws URISyntaxException {
        final SendFileServer server = new SendFileServer(new URI("natty://127.0.0.1:7777"));
        final Runnable runner = new Runnable() {
            
            @Override
            public void run() {
                try {
                    server.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        final Thread t = new Thread(runner, "SendFileTestServer");
        t.setDaemon(true);
        t.start();
    }

}

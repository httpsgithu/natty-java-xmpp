package org.lantern.natty;

import java.io.File;

public class SendFileTest {

    public static void main(String[] args) throws Exception {
        startNettyServer();
        Thread.sleep(300);
        final File file = new File("pom.xml");
        final SendFileClient client = new SendFileClient("127.0.0.1", 7777, file);
        client.run();
    }

    private static void startNettyServer() {
        final SendFileServer server = new SendFileServer(7777);
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

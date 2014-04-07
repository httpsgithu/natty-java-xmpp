package org.lantern.natty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;
import java.util.concurrent.ThreadFactory;

/**
 * UDT client for sending a file.
 */
public class SendFileClient {


    private final String host;
    private final int port;

    private final File file;

    public SendFileClient(final String host, final int port,
            final File file) {
        this.host = host;
        this.port = port;
        this.file = file;
    }

    public void run() throws Exception {
        // Configure the client.
        final ThreadFactory connectFactory = new UtilThreadFactory("connect");
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1,
                connectFactory, NioUdtProvider.BYTE_PROVIDER);
        try {
            final Bootstrap boot = new Bootstrap();
            boot.group(connectGroup)
                    .channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                    .handler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new SendFileClientHandler(file));
                        }
                    });
            // Start the client.
            final ChannelFuture f = boot.connect(host, port).sync();
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            connectGroup.shutdownGracefully();
        }
    }
}

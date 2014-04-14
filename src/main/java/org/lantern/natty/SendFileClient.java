package org.lantern.natty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UDT client for sending a file.
 */
public class SendFileClient {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final File file;
    private final URI local;
    private final URI remote;

    public SendFileClient(final URI localAddress, final URI remoteAddress,
            final File file) {
        this.local = localAddress;
        this.remote = remoteAddress;
        this.file = file;
    }

    public void run() throws Exception {
        log.debug("Starting send file client... from {} to {}", local, remote);
        // Configure the client.
        final ThreadFactory connectFactory = new UtilThreadFactory("connect");
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1,
                connectFactory, NioUdtProvider.BYTE_PROVIDER);
        try {
            final Bootstrap boot = new Bootstrap();
            boot.group(connectGroup)
                    .channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                    .option(ChannelOption.SO_REUSEADDR, true)
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
            boot.bind(this.local.getHost(), this.local.getPort());
            final ChannelFuture f = 
                boot.connect(this.remote.getHost(), this.remote.getPort()).sync();
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            connectGroup.shutdownGracefully();
        }
    }
}

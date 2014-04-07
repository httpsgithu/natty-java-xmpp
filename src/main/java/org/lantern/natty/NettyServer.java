package org.lantern.natty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple server that receives files.
 */
public class NettyServer {
    
    private int port;

    public NettyServer(final int port) {
        this.port = port;
    }

    public void run() throws Exception {
        
        final ThreadFactory acceptFactory = new ThreadFactory() {
            
            private final AtomicInteger count = new AtomicInteger(0);
            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = 
                        new Thread(r, "Netty-Server-Accept-"+count.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        
        final ThreadFactory connectFactory = new ThreadFactory() {
            
            private final AtomicInteger count = new AtomicInteger(0);
            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = 
                        new Thread(r, "Netty-Server-Connect-"+count.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        
        final NioEventLoopGroup acceptGroup = new NioEventLoopGroup(1,
                acceptFactory, NioUdtProvider.BYTE_PROVIDER);
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1,
                connectFactory, NioUdtProvider.BYTE_PROVIDER);
        // Configure the server.
        try {
            final ServerBootstrap boot = new ServerBootstrap();
            boot.group(acceptGroup, connectGroup)
                    .channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                    .option(ChannelOption.SO_BACKLOG, 10)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new ByteEchoServerHandler());
                        }
                    });
            // Start the server.
            final ChannelFuture future = boot.bind(port).sync();
            // Wait until the server socket is closed.
            future.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            acceptGroup.shutdownGracefully();
            connectGroup.shutdownGracefully();
        }
    }
    
    /**
     * Handler implementation for the file receiving server.
     */
    @Sharable
    public static class ByteEchoServerHandler extends ChannelInboundHandlerAdapter {

        private static final Logger logger = 
                Logger.getLogger(ByteEchoServerHandler.class.getName());

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            logger.info("Received data!!! ");
            // Discard the received data silently.
            ((ByteBuf) msg).release(); 
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx,
                final Throwable cause) {
            logger.log(Level.WARNING, "close the connection when an exception is raised", cause);
            ctx.close();
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            logger.info("ECHO active " + NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
        }
    }
}

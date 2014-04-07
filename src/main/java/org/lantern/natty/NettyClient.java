package org.lantern.natty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class NettyClient {


    private final String host;
    private final int port;
    private final File file;

    public NettyClient(final String host, final int port, final File file) {
        this.host = host;
        this.port = port;
        this.file = file;
    }
    
    public void run() throws Exception {
        // Configure the client.
        final ThreadFactory connectFactory = new ThreadFactory() {
            
            private final AtomicInteger count = new AtomicInteger(0);
            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = 
                        new Thread(r, "Netty-Client-"+count.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
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
                                    new FileSendingClientHandler(file));
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
    
    private class FileSendingClientHandler extends
            SimpleChannelInboundHandler<ByteBuf> {

        final MetricRegistry metrics = new MetricRegistry();
        
        private final Logger log = Logger
                .getLogger(FileSendingClientHandler.class.getName());

        private final Meter meter = 
                this.metrics.meter(MetricRegistry.name(getClass(), "bytes"));

        private final File fileToSend;

        public FileSendingClientHandler(final File file) {
            super(false);
            this.fileToSend = file;

        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx)
                throws Exception {
            log.info("File send channel active "
                    + NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
            
            if (fileToSend.exists()) {
                if (!fileToSend.isFile()) {
                    ctx.writeAndFlush("Not a file: " + fileToSend + '\n');
                    return;
                }
                ctx.write(fileToSend + " " + fileToSend.length() + '\n');
                final FileInputStream fis = new FileInputStream(fileToSend);
                final FileRegion region = 
                        new DefaultFileRegion(fis.getChannel(), 0, fileToSend.length());
                ctx.write(region);
                ctx.writeAndFlush("\n");
                fis.close();
            } else {
                System.err.println("NO FILE AT '"+fileToSend.getAbsolutePath()+"'");
                ctx.writeAndFlush("File not found: " + fileToSend + '\n');
            }
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)
                throws Exception {
            meter.mark(msg.readableBytes());

            ctx.write(msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx)
                throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx,
                final Throwable cause) {
            log.log(Level.WARNING,
                    "close the connection when an exception is raised", cause);
            ctx.close();
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}

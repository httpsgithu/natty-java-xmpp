package org.lantern.natty;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.udt.nio.NioUdtProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

/**
 * Handler implementation for sending a file.
 */
public class SendFileClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = Logger.getLogger(SendFileClientHandler.class.getName());

    private final ByteBuf message;

    final Meter meter = Metrics.newMeter(SendFileClientHandler.class, "rate",
            "bytes", TimeUnit.SECONDS);

    public SendFileClientHandler(final File file) {
        super(false);

        message = Unpooled.buffer((int) file.length());

        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            for (int i = 0; i < message.capacity(); i++) {
                message.writeByte(is.read());
            }
        } catch (final IOException ioe) {
            
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        log.info("Send file client channel active " + NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
        ctx.writeAndFlush(message);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        meter.mark(msg.readableBytes());
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final Throwable cause) {
        log.log(Level.WARNING, "close the connection when an exception is raised", cause);
        ctx.close();
    }

}

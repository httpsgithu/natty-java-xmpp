package org.lantern.natty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.udt.nio.NioUdtProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler implementation for the file sending server.
 */
@Sharable
public class SendFileServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = Logger.getLogger(SendFileServerHandler.class.getName());

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        log.info("SERVER READ");
        
        final ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        
        final File bop = new File("UDT-File-Sending-Demo-File");
        FileOutputStream os = null;
        os = new FileOutputStream(bop, true);
        os.write(bytes);
        os.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel READ COMPLETE");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final Throwable cause) {
        log.log(Level.WARNING, "close the connection when an exception is raised", cause);
        ctx.close();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        log.info("Send file server channel active " + NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
    }

}

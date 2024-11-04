package org.example.channelHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.example.channelHandler.handler.MySimpleChannelInboundHandler;

/**
 * @author xiaonaol
 * @date 2024/11/4
 **/
public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new MySimpleChannelInboundHandler());
    }
}

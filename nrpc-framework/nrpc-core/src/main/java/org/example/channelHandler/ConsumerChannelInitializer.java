package org.example.channelHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.example.channelHandler.handler.MySimpleChannelInboundHandler;
import org.example.channelHandler.handler.NrpcMessageEncoder;

/**
 * @author xiaonaol
 * @date 2024/11/4
 **/
public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                // netty自带的日志处理器
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                // 消息编码器
                .addLast(new NrpcMessageEncoder())
                .addLast(new MySimpleChannelInboundHandler());
    }
}

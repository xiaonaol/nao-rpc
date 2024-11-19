package org.example.channelHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.example.channelHandler.handler.MySimpleChannelInboundHandler;
import org.example.channelHandler.handler.NrpcRequestEncoder;
import org.example.channelHandler.handler.NrpcResponseDecoder;

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
                .addLast(new NrpcRequestEncoder())
                // 入站的解码器
                .addLast(new NrpcResponseDecoder())
                // 处理结果
                .addLast(new MySimpleChannelInboundHandler());
    }
}

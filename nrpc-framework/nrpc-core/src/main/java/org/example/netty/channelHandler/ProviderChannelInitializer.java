package org.example.netty.channelHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.example.netty.channelHandler.handler.providerHandler.MethodCallHandler;
import org.example.netty.channelHandler.handler.providerHandler.NrpcRequestDecoder;
import org.example.netty.channelHandler.handler.providerHandler.NrpcResponseEncoder;

/**
 * @author xiaonaol
 * @date 2024/12/17
 **/
public class ProviderChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                // netty自带的日志处理器
                .addLast(new LoggingHandler())
                // 请求解码器
                .addLast(new NrpcRequestDecoder())
                // 方法调用
                .addLast(new MethodCallHandler())
                // 响应编码器
                .addLast(new NrpcResponseEncoder());
    }
}

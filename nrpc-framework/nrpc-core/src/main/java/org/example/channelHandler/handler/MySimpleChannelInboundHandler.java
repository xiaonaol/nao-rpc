package org.example.channelHandler.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.example.NrpcBootstrap;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 测试类
 * @author xiaonaol
 * @date 2024/11/4
 **/
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        // 服务提供方，给与的结果
        String result = msg.toString(Charset.defaultCharset());
        CompletableFuture<Object> completableFuture = NrpcBootstrap.PENDING_QUEST.get(1L);
        completableFuture.complete(result);
    }
}

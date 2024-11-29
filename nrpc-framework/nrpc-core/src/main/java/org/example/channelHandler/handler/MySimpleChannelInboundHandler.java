package org.example.channelHandler.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.example.NrpcBootstrap;
import org.example.transport.message.NrpcResponse;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 测试类
 * @author xiaonaol
 * @date 2024/11/4
 **/
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<NrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NrpcResponse nrpcResponse) throws Exception {

        // 服务提供方，给与的结果
        Object returnValue = nrpcResponse.getBody();

        // todo 需要针对code做处理
        returnValue = returnValue == null ? new Object() : returnValue;

        // 从全局挂起的请求中寻找与之匹配的completableFuture
        CompletableFuture<Object> completableFuture = NrpcBootstrap.PENDING_QUEST.get(nrpcResponse.getRequestId());
        completableFuture.complete(returnValue);
        if(log.isDebugEnabled()){
            log.debug("已寻找到编号为【{}】的completableFuture结果", nrpcResponse.getRequestId());
        }
    }
}

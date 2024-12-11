package org.example.channelHandler.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.example.NrpcBootstrap;
import org.example.enumeration.RespCode;
import org.example.exceptions.ResponseException;
import org.example.protection.CircuitBreaker;
import org.example.transport.message.NrpcResponse;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 测试类
 * @author xiaonaol
 * @date 2024/11/4
 **/
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<NrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NrpcResponse nrpcResponse) throws ResponseException {

        // 从全局挂起的请求中寻找与之匹配的completableFuture
        CompletableFuture<Object> completableFuture = NrpcBootstrap.PENDING_QUEST.get(nrpcResponse.getRequestId());

        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, CircuitBreaker> ipCircuitBreaker = NrpcBootstrap.getInstance()
                .getConfiguration().getIpCircuitBreaker();
        CircuitBreaker circuitBreaker = ipCircuitBreaker.get(socketAddress);

        byte code = nrpcResponse.getCode();
        if(code == RespCode.FAIL.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，返回错误的结果，响应码【{}】",
                    nrpcResponse.getRequestId(), code);
            throw new ResponseException(code, RespCode.FAIL.getDesc());
        } else if(code == RespCode.RATE_LIMIT.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求被限流，响应码【{}】",
                    nrpcResponse.getRequestId(), code);
            throw new ResponseException(code, RespCode.RATE_LIMIT.getDesc());
        } else if(code == RespCode.RESOURCE_NOT_FOUND.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，未找到目标资源，响应码【{}】",
                    nrpcResponse.getRequestId(), code);
            throw new ResponseException(code, RespCode.RESOURCE_NOT_FOUND.getDesc());
        } else if(code == RespCode.SUCCESS_HEARTBEAT.getCode()) {
            completableFuture.complete(null);
            log.error("当前id为【{}】的心跳请求，响应码【{}】",
                    nrpcResponse.getRequestId(), code);
        } else if(code == RespCode.SUCCESS.getCode()) {
            // 服务提供方，给与的结果
            Object returnValue = nrpcResponse.getBody();

            // todo 需要针对code做处理
            returnValue = returnValue == null ? new Object() : returnValue;

            completableFuture.complete(returnValue);
            if(log.isDebugEnabled()){
                log.debug("已寻找到编号为【{}】的completableFuture结果", nrpcResponse.getRequestId());
            }
        }
    }
}

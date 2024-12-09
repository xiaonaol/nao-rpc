package org.example.channelHandler.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.example.NrpcBootstrap;
import org.example.ServiceConfig;
import org.example.enumeration.RequestType;
import org.example.enumeration.RespCode;
import org.example.protection.RateLimiter;
import org.example.protection.TokenBuketRateLimiter;
import org.example.transport.message.NrpcRequest;
import org.example.transport.message.NrpcResponse;
import org.example.transport.message.RequestPayload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * @author xiaonaol
 * @date 2024/11/18
 **/
@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<NrpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NrpcRequest nrpcRequest) throws Exception {
        // 先封装部分响应
        NrpcResponse nrpcResponse = new NrpcResponse();
        nrpcResponse.setRequestId(nrpcRequest.getRequestId());
        nrpcResponse.setCompressType(nrpcRequest.getCompressType());
        nrpcResponse.setSerializeType(nrpcRequest.getSerializeType());

        // 1、完成限流相关的操作
        Channel channel = channelHandlerContext.channel();
        SocketAddress socketAddress = channel.remoteAddress();
        Map<SocketAddress, RateLimiter> ipRateLimiter =
                NrpcBootstrap.getInstance().getConfiguration().getIpRateLimiter();

        RateLimiter rateLimiter = ipRateLimiter.get(socketAddress);
        if(rateLimiter == null) {
            rateLimiter = new TokenBuketRateLimiter(500, 300);
            ipRateLimiter.put(socketAddress, rateLimiter);
        }

        boolean allowRequest = rateLimiter.allowRequest();

        // 限流
        if(!allowRequest) {
            // 需要封装响应并且返回
            nrpcResponse.setCode(RespCode.RATE_LIMIT.getCode());
        } else if(nrpcRequest.getRequestType() == RequestType.HEART_BEAT.getId()) {
            // 需要封装响应并且返回
            nrpcResponse.setCode(RespCode.SUCCESS.getCode());

        } else {
            /** ------具体调用过程------ **/

            // 1. 获取payload内容
            RequestPayload requestPayload = nrpcRequest.getRequestPayload();

            // 2. 根据payload内容进行方法调用
            try {
                Object result = callTargetMethod(requestPayload);
                if (log.isDebugEnabled()) {
                    log.debug("【{}】已在服务端完成调用", nrpcRequest.getRequestId());
                }
                // 3. 封装响应
                nrpcResponse.setCode(RespCode.SUCCESS.getCode());
                nrpcResponse.setBody(result);
            } catch (Exception e) {
                nrpcResponse.setCode(RespCode.FAIL.getCode());
                log.error("编号为【{}】的请求在调用时发生异常", nrpcRequest.getRequestId(), e);
            }
        }

        // 4. 写出响应
        // todo why not "channelHandlerContext.writeAndFlush(nrpcResponse);" ?
        channelHandlerContext.channel().writeAndFlush(nrpcResponse);
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parametersValue = requestPayload.getParametersValue();

        // 寻找到匹配的暴露出去的具体的实现
        ServiceConfig<?> serviceConfig = NrpcBootstrap.SERVERS_LIST.get(interfaceName);
        Object refImpl = serviceConfig.getRef();

        // 通过反射调用 1、获取方法对象 2、执行invoke方法
        Object returnValue = null;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parametersType);
            returnValue = method.invoke(refImpl, parametersValue);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error("调用服务【{}】的方法【{}】时发生了异常", interfaceName, methodName, e);
            throw new RuntimeException(e);
        }


        return returnValue;
    }
}

package org.example.channelHandler.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.example.NrpcBootstrap;
import org.example.ServiceConfig;
import org.example.transport.message.NrpcRequest;
import org.example.transport.message.RequestPayload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author xiaonaol
 * @date 2024/11/18
 **/
@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<NrpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NrpcRequest nrpcRequest) throws Exception {
        // 1. 获取payload内容
        RequestPayload requestPayload = nrpcRequest.getRequestPayload();

        // 2. 根据payload内容进行方法调用
        Object object = callTargetMethod(requestPayload);
        System.out.println(object + "!!!!!!!!!!!!");

        // 3. 封装响应

        // 4. 写出响应
        channelHandlerContext.writeAndFlush(object);
        channelHandlerContext.channel().writeAndFlush(object);
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

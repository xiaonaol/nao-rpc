package org.example.proxy.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.server.Request;
import org.example.IdGenerator;
import org.example.NettyBootstrapInitializer;
import org.example.NrpcBootstrap;
import org.example.discovery.Registry;
import org.example.enumeration.RequestType;
import org.example.exceptions.DiscoveryException;
import org.example.exceptions.NetworkException;
import org.example.serialize.SerializerFactory;
import org.example.transport.message.NrpcRequest;
import org.example.transport.message.RequestPayload;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 该类封装了客户端通信的基础逻辑，每一个代理对象的远程调用过程都封装在了invoke方法中
 * 1、发现可用服务 2、建立连接 3、发送请求 4、得到结果
 * @author xiaonaol
 * @date 2024/11/4
 **/
@Slf4j
@Builder
public class RpcConsumerInvocationHandler implements InvocationHandler {
    // 注册中心和接口
    private final Registry registry;
    private final Class<?> interfaceRef;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 我们调用saHi方法，事实上会走进这个代码段中
        // 我们已经知道 method（具体的方法），args(参数列表)
        log.info("method-->{}", method.getName());
        log.info("args-->{}", args);

        // 1、发现服务，从注册中心，寻找一个可用的服务
        // 传入服务的名字,返回ip+端口
        InetSocketAddress address = registry.lookup(interfaceRef.getName());
        if (log.isDebugEnabled()) {
            log.debug("服务调用方，发现了服务【{}】的可用主机【{}】.",
                    interfaceRef.getName(), address);
        }

        // 2、尝试获取一个可用通道
        Channel channel = getAvailableChannel(address);
        if(log.isDebugEnabled()) {
            log.debug("获取了和【{}】建立的连接通道，准备发送数据", address);
        }

        // 3、
        /*
         * ------------------ 封装报文 ---------------------------
         */
        RequestPayload requestPayload = RequestPayload.builder()
                .interfaceName(interfaceRef.getName())
                .methodName(method.getName())
                .parametersType(method.getParameterTypes())
                .parametersValue(args)
                .returnType(method.getReturnType())
                .build();

        // TODO 需要对各种请求id和各种类型做区分
        NrpcRequest nrpcRequest = NrpcRequest.builder()
                .requestId(NrpcBootstrap.idGenerator.getId())
                .compressType((byte) 1)
                .requestType(RequestType.REQUEST.getId())
                .serializeType(SerializerFactory.getSerializer(NrpcBootstrap.SERIALIZE_TYPE).getCode())
                .requestPayload(requestPayload)
                .build();


        /*
         * ------------------异步策略-------------------------
         */

        // 4、写出报文
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        // 将completableFuture暴露
        NrpcBootstrap.PENDING_QUEST.put(1L, completableFuture);

        // 这里直接writeAndFlush写出了一个请求，这个请求的实例就会进入pipline执行出站的一系列操作
        // 我们可以想象到，第一个出站程序一定是将 nrpcRequest -> 二进制报文
        channel.writeAndFlush(nrpcRequest).addListener((ChannelFutureListener) promise -> {
            // 只需要处理以下异常就行了
            if (!promise.isSuccess()) {
                completableFuture.completeExceptionally(promise.cause());
            }
        });
//
        // 如果没有地方处理这个 completableFuture ，这里会阻塞，等待complete方法的执行
        // q: 我们需要在哪里调用complete方法得到结果，很明显 pipeline 中最终的handler的处理结果

        // 阻塞等待在pipeline中handler最后执行complete方法
        // 5、获得响应的结果
        return completableFuture.get(10, TimeUnit.SECONDS);
    }


    /**
     * 根据地址获取一个可用的通道
     * @param address 地址
     * @return Channel 可用通道
     * @author xiaonaol
     */
    private Channel getAvailableChannel(InetSocketAddress address) {
        // 1、尝试从缓存中获取
        Channel channel = NrpcBootstrap.CHANNEL_CACHE.get(address);

        // 2、拿不到就去建立连接
        if (channel == null) {
            // await 方法会阻塞，会等待连接成功在返回，netty还提供了异步处理的逻辑
            // sync和await都是阻塞当前线程，获取返回值（连接的过程是异步的，发生数据的过程是异步的）
            // 如果发生了异常，sync会主动在主线程抛出异常，await不会，异常在子线程中处理需要使用future中处理
            // 使用addListener执行的异步操作
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getBootstrap().connect(address).addListener(
                    (ChannelFutureListener) promise -> {
                        if (promise.isDone()) {
                            // 异步的，我们已经完成
                            if (log.isDebugEnabled()) {
                                log.debug("已经和【{}】成功建立了连接。", address);
                            }
                            channelFuture.complete(promise.channel());
                        } else if (!promise.isSuccess()) {
                            channelFuture.completeExceptionally(promise.cause());
                        }
                    }
            );

            // 阻塞获取channel
            try {
                channel = channelFuture.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取通道时，发生异常：", e);
                throw new DiscoveryException(e);
            }

            // 缓存channel
            NrpcBootstrap.CHANNEL_CACHE.put(address, channel);
        }

        if (channel == null) {
            log.error("获取或建立与【{}】的通道时发生了异常。", address);
            throw new NetworkException("获取通道时发生了异常。");
        }

        return channel;
    }
}

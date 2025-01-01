package org.example.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.example.netty.NrpcUtils;
import org.example.netty.initializer.NettyBootstrapInitializer;
import org.example.NrpcBootstrap;
import org.example.annotation.TryTimes;
import org.example.compress.CompressorFactory;
import org.example.discovery.Registry;
import org.example.enumeration.RequestType;
import org.example.exceptions.DiscoveryException;
import org.example.exceptions.NetworkException;
import org.example.protection.CircuitBreaker;
import org.example.serialize.SerializerFactory;
import org.example.transport.message.NrpcRequest;
import org.example.transport.message.RequestPayload;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
public class RpcConsumerInvocationHandler implements InvocationHandler {
    // 注册中心和接口
    private final Registry registry;
    private final Class<?> interfaceRef;
    private String group;
    private InetSocketAddress address;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef, String group) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
        this.group = group;

        // 传入服务的名字,返回ip+端口
        this.address = NrpcBootstrap.getInstance()
                .getConfiguration().getLoadBalancer().selectServiceAddress(interfaceRef.getName(), group);
        if (log.isDebugEnabled()) {
            log.debug("服务调用方，发现了服务【{}】的可用主机【{}】.",
                    interfaceRef.getName(), address);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        TryTimes tryTimesAnnotation = method.getAnnotation(TryTimes.class);

        // 默认0代表不重试
        int maxRetry = 0;
        int intervalTime = 1000;
        if (tryTimesAnnotation != null) {
            maxRetry = tryTimesAnnotation.tryTimes();
            intervalTime = tryTimesAnnotation.intervalTime();
        }

        // 发送调用请求
        try {
            return executeWithRetry(method, args, maxRetry, intervalTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Object executeWithRetry(Method method, Object[] args, int maxRetry, int intervalTime) throws InterruptedException {

        // 4、获取当前地址对应的断路器，如果断路器打开则不发送请求
        Map<SocketAddress, CircuitBreaker> ipCircuitBreaker = NrpcBootstrap.getInstance()
                .getConfiguration().getIpCircuitBreaker();
        CircuitBreaker circuitBreaker = ipCircuitBreaker.get(address);
        if(circuitBreaker == null) {
            circuitBreaker = new CircuitBreaker(10, 0.5F);
            ipCircuitBreaker.put(address, circuitBreaker);
        }

        // 如果断路器是打开的
        if(circuitBreaker.isBreak()) {
            // 定期打开
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    NrpcBootstrap.getInstance()
                            .getConfiguration().getIpCircuitBreaker().get(address).reset();
                }
            }, 5000);

            throw new RuntimeException("断路器开启，无法发送请求");
        }

        int attemps = 0;
        while (attemps <= maxRetry) {
            try {
                return executeRequest(method, args);
            } catch (Exception e) {
                if (++ attemps == maxRetry) {
                    log.error("对方法【{}】进行远程调用，重试{}次，依然不可调用",
                            method.getName(), maxRetry);
                }
                log.error("在进行第{}次重试时发生异常：", attemps, e);

                // 记录错误的次数
                circuitBreaker.recordErrorRequest();
                Thread.sleep(intervalTime);
            }
        }
        throw new RuntimeException("执行远程方法" + method.getName() + "调用失败。");
    }

    private Object executeRequest(Method method, Object[] args) throws ExecutionException, InterruptedException, TimeoutException {
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

        /*
         * ------------------ 创建请求 ---------------------------
         */
        NrpcRequest nrpcRequest = NrpcRequest.builder()
                .requestId(NrpcBootstrap.getInstance().getConfiguration().getIdGenerator().getId())
                .compressType(CompressorFactory.getCompressor(NrpcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                .requestType(RequestType.REQUEST.getId())
                .serializeType(SerializerFactory.getSerializer(NrpcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                .timeStamp(System.currentTimeMillis())
                .requestPayload(requestPayload)
                .build();

        // 将请求存入本地线程，需要在合适的时候调用remove方法
        NrpcBootstrap.REQUEST_THREAD_LOCAL.set(nrpcRequest);

        // 尝试获取一个可用通道
        Channel channel = getAvailableChannel(address);
        if (log.isDebugEnabled()) {
            log.debug("获取了和【{}】建立的连接通道，准备发送数据", address);
        }

        /*
         * ------------------异步策略-------------------------
         */

        // 写出报文
        CompletableFuture<Object> completableFuture =
                NrpcUtils.sendRequest(channel, nrpcRequest, NrpcBootstrap.PENDING_QUEST);

        // 清理ThreadLocal
        NrpcBootstrap.REQUEST_THREAD_LOCAL.remove();

        // 获得响应的结果
        Object result = completableFuture.get(10, TimeUnit.SECONDS);

        // 记录成功的请求
        return result;
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

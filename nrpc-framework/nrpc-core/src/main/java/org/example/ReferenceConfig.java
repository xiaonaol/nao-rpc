package org.example;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.example.discovery.Registry;
import org.example.discovery.RegistryConfig;
import org.example.exceptions.NetworkException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
@Slf4j
public class ReferenceConfig<T> {
    private Class<T> interfaceRef;

    private Registry registry;

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    /**
     * 代理设计模式，生成一个api接口的代理对象，helloYrpc.sayHi("你好");
     *
     * @return 代理对象
     */
    public T get() {
        // 次出一定是使用动态代理完成了一些工作
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceRef};

        // 使用动态代理生成代理对象
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
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

                // 2、使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果
                // 定义线程池，EventLoopGroup
                // q：整个连接过程放在这里行不行，也就意味着每次调用都会产生一个新的netty连接。如何缓存我们的连接
                // 也就意味着，每次在此处建立一个新的连接是不合适的


                // 解决方案？缓存channel，尝试从缓存中获取channel，如果未获取，则创建新的连接，并进行缓存
                // 1、尝试从全局的缓存中获取一个通道
                Channel channel = NrpcBootstrap.CHANNEL_CACHE.get(address);

                if (channel == null) {
                    // await 方法会阻塞，会等待连接成功在返回，netty还提供了异步处理的逻辑
                    // sync和await都是阻塞当前线程，获取返回值（连接的过程是异步的，发生数据的过程是异步的）
                    // 如果发生了异常，sync会主动在主线程抛出异常，await不会，异常在子线程中处理需要使用future中处理
//                    channel = NettyBootstrapInitializer.getBootstrap()
//                        .connect(address).await().channel();

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
                    channel = channelFuture.get(3, TimeUnit.SECONDS);

                    // 缓存channel
                    NrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }

                if (channel == null) {
                    log.error("获取或建立与【{}】的通道时发生了异常。", address);
                    throw new NetworkException("获取通道时发生了异常。");
                }


                /*
                 * ------------------ 封装报文 ---------------------------
                 */

                /*
                 * ------------------同步策略-------------------------
                 */
//                ChannelFuture channelFuture = channel.writeAndFlush(new Object()).await();
                // 需要学习channelFuture的简单的api get 阻塞获取结果，getNow 获取当前的结果，如果未处理完成，返回null
//                if(channelFuture.isDone()){
//                    Object object = channelFuture.getNow();
//                } else if( !channelFuture.isSuccess() ){
//                    // 需要捕获异常,可以捕获异步任务中的异常
//                    Throwable cause = channelFuture.cause();
//                    throw new RuntimeException(cause);
//                }

                /*
                 * ------------------异步策略-------------------------
                 */
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();

                channel.writeAndFlush(Unpooled.copiedBuffer("hello".getBytes())).addListener((ChannelFutureListener) promise -> {
                    // 当前的promise将来返回的结果是什么，writeAndFlush的返回结果
                    // 一旦数据被写出去，这个promise也就结束了
                    // 但是我们想要的是什么？  服务端给我们的返回值，所以这里处理completableFuture是有问题的
                    // 是不是应该将 completableFuture 挂起并且暴露，并且在得到服务提供方的响应的时候调用complete方法
//                    if(promise.isDone()){
//                        completableFuture.complete(promise.getNow());
//                    }
                    System.out.println("消息发送了！");
                    // 只需要处理以下异常就行了
                    if (!promise.isSuccess()) {
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });
//
                // 如果没有地方处理这个 completableFuture ，这里会阻塞，等待complete方法的执行
                // q: 我们需要在哪里调用complete方法得到结果，很明显 pipeline 中最终的handler的处理结果

                return null;
            }
        });
        return (T) helloProxy;
    }


    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterfaceRef(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
}

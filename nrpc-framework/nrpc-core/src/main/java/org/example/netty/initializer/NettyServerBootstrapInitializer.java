package org.example.netty.initializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.netty.channelHandler.ProviderChannelInitializer;
import org.example.core.NrpcShutdownHook;

/**
 * @author xiaonaol
 * @date 2024/12/17
 **/
public class NettyServerBootstrapInitializer {
    private static final ServerBootstrap serverBootstrap = new ServerBootstrap();
    public static final EventLoopGroup boss;
    public static final EventLoopGroup worker;

    static {
        // 注册一个关闭应用程序的Hook函数
        Runtime.getRuntime().addShutdownHook(new NrpcShutdownHook());

        // 1、创建eventLoop，老板只负责处理请求，之后会将请求分发至worker
        boss = new NioEventLoopGroup(2);
        worker = new NioEventLoopGroup(10);

        serverBootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ProviderChannelInitializer());
    }

    private NettyServerBootstrapInitializer() {

    }

    public static ServerBootstrap getServerBootstrap() {
        return serverBootstrap;
    }
}

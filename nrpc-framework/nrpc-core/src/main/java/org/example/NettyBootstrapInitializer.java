package org.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * @author xiaonaol
 * @date 2024/11/3
 **/
public class NettyBootstrapInitializer {
    private static final Bootstrap bootstrap = new Bootstrap();

    static {
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                // 选择初始化一个什么样的channel
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
                                // 服务提供方，给与的结果
                                String result = msg.toString(Charset.defaultCharset());
                                System.out.println(result);
                            }
                        });
                    }
                });
    }

    private NettyBootstrapInitializer() {
    }

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }
}

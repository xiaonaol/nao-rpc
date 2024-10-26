package org.example.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class AppClient implements Serializable {
    public void run() {
        // 定义线程池
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            // 启动一个客户端需要一个辅助类
            Bootstrap bootstrap = new Bootstrap();
            bootstrap = bootstrap.group(group)
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                    // 选择初始化一个Channel
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new MyClientChannelHandler());
                        }
                    });

            // 尝试连接服务器
            ChannelFuture channelFuture = null;
            channelFuture = bootstrap.connect().sync();
            // 获取channel，并且写出数据
            channelFuture.channel().writeAndFlush(Unpooled.copiedBuffer("hello netty".getBytes(StandardCharsets.UTF_8)));
            // 阻塞程序，等待接受消息
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                group.shutdownGracefully();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new AppClient().run();
    }
}

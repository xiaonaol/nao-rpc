package org.example.core;

import org.example.netty.initializer.NettyServerBootstrapInitializer;

/**
 * @author xiaonaol
 * @date 2024/12/11
 **/
public class NrpcShutdownHook extends Thread {
    @Override
    public void run() {
        // 1、打开挡板
        ShutdownHolder.BAFFLE.set(true);

        // 2、等待计数器归零（正常的请求处理结束）
        // 等待计数器归零
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(ShutdownHolder.LATCH.sum() == 0L
            || System.currentTimeMillis() - start > 10000) {
                break;
            }
        }

        // 3、阻塞结束后放行，执行其他操作
        NettyServerBootstrapInitializer.boss.shutdownGracefully();
        NettyServerBootstrapInitializer.worker.shutdownGracefully();
    }
}

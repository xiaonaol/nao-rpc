package org.example.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.example.transport.message.NrpcRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author xiaonaol
 * @date 2025/1/1
 **/
public class NrpcUtils {
    public static <T> CompletableFuture<Object> sendRequest(Channel channel, NrpcRequest nrpcRequest,
                                                            Map<Long, CompletableFuture<Object>> pendingQuests) {
        // 写出报文
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();

        // 将completableFuture暴露
        pendingQuests.put(nrpcRequest.getRequestId(), completableFuture);

        // 这里直接writeAndFlush写出了一个请求，这个请求的实例就会进入pipline执行出站的一系列操作
        channel.writeAndFlush(nrpcRequest).addListener((ChannelFutureListener) promise -> {
            // 只需要处理以下异常就行了
            if (!promise.isSuccess()) {
                completableFuture.completeExceptionally(promise.cause());
            }
        });

        return completableFuture;
    }
}

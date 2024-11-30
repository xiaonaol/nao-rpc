package org.example.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.example.NettyBootstrapInitializer;
import org.example.NrpcBootstrap;
import org.example.compress.CompressorFactory;
import org.example.discovery.Registry;
import org.example.enumeration.RequestType;
import org.example.serialize.SerializerFactory;
import org.example.transport.message.NrpcRequest;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 探测服务是死是活
 * @author xiaonaol
 * @date 2024/11/29
 **/
@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String serviceName) {
        // 从注册中心拉取服务列表并建立连接
        Registry registry = NrpcBootstrap.getInstance().getRegistry();
        List<InetSocketAddress> addresses = registry.lookup(serviceName);

        // 将连接进行缓存
        for(InetSocketAddress address : addresses) {
            try {
                if(!NrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    Channel channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    NrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 定期发送消息
        Thread thread = new Thread(() ->
            new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 2000)
        , "nrpc-HeartbeatDetector-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private static class MyTimerTask extends TimerTask {

        @Override
        public void run() {

            // 将响应时长的map清空
            NrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();

            // 遍历所有channel
            Map<InetSocketAddress, Channel> cache = NrpcBootstrap.CHANNEL_CACHE;
            for(Map.Entry<InetSocketAddress, Channel> entry : cache.entrySet()) {
                // 定义重试剩余次数
                int tryTimes = 3;
                while(tryTimes > 0) {

                    // 通过心跳检测处理每一个channel
                    Channel channel = entry.getValue();

                    long start = System.currentTimeMillis();
                    // 构建一个心跳请求
                    NrpcRequest nrpcRequest = NrpcRequest.builder()
                            .requestId(NrpcBootstrap.ID_GENERATOR.getId())
                            .compressType(CompressorFactory.getCompressor(NrpcBootstrap.COMPRESS_TYPE).getCode())
                            .requestType(RequestType.HEART_BEAT.getId())
                            .serializeType(SerializerFactory.getSerializer(NrpcBootstrap.SERIALIZE_TYPE).getCode())
                            .timeStamp(start)
                            .build();

                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                    // 将completableFuture暴露
                    NrpcBootstrap.PENDING_QUEST.put(nrpcRequest.getRequestId(), completableFuture);

                    channel.writeAndFlush(nrpcRequest).addListener((ChannelFutureListener) promise -> {
                        if (!promise.isSuccess()) {
                            completableFuture.completeExceptionally(promise.cause());
                        }
                    });

                    Long endTime = 0L;
                    try {
                        completableFuture.get(1, TimeUnit.SECONDS);
                        endTime = System.currentTimeMillis();
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        // 一旦发生问题，需要优先重试
                        tryTimes --;
                        log.error("和地址为【{}】的主机连接发生异常，正在进行【{}】次重试...", channel.remoteAddress(), 3 - tryTimes);

                        // 重试用尽，将失效的地址移出服务列表
                        if(tryTimes == 0) {
                            NrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                            log.error("将地址为【{}】的主机移出服务列表", channel.remoteAddress());
                        }

                        // 尝试等待一段时间后重试
                        try {
                            Thread.sleep(10*(new Random().nextInt(5)));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }

                        continue;
                    }
                    Long time = endTime - start;

                    // 使用treemap进行缓存
                    NrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time, channel);
                    break;
                }
            }

            log.info("--------------------响应时间的treemap------------------");
            for (Map.Entry<Long, Channel> entry : NrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet()) {
                log.info("[{}] ----> channel_id: [{}]", entry.getKey(), entry.getValue().id());
            }
        }
    }

}

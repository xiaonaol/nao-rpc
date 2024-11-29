package org.example.loadbalancer.impl;

import io.netty.channel.Channel;
import org.example.NrpcBootstrap;
import org.example.exceptions.LoadBalancerException;
import org.example.loadbalancer.AbstractLoadBalancer;
import org.example.loadbalancer.Selector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xiaonaol
 * @date 2024/11/30
 **/
public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinimumResponseTimeSelector();
    }

    private static class MinimumResponseTimeSelector implements Selector {

        public MinimumResponseTimeSelector() {
        }

        @Override
        public InetSocketAddress getNext() {
            Map.Entry<Long, Channel> entry = NrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if(entry != null) {
                return (InetSocketAddress) NrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry().getValue().remoteAddress();
            }

            // 直接从缓存中获取一个可用的
            Channel channel = (Channel) NrpcBootstrap.CHANNEL_CACHE.values().toArray()[0];
            return (InetSocketAddress) channel.remoteAddress();
        }

        @Override
        public void reBalance() {

        }
    }
}

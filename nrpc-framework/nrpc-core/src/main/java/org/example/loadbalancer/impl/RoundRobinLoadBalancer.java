package org.example.loadbalancer.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.NrpcBootstrap;
import org.example.discovery.Registry;
import org.example.exceptions.LoadBalancerException;
import org.example.loadbalancer.AbstractLoadBalancer;
import org.example.loadbalancer.LoadBalancer;
import org.example.loadbalancer.Selector;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询的负载均衡策略
 * @author xiaonaol
 * @date 2024/11/24
 **/
@Slf4j
public class RoundRobinLoadBalancer extends AbstractLoadBalancer{

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new RoundRobinSelector(serviceList);
    }

    private static class RoundRobinSelector implements Selector {

        private List<InetSocketAddress> serviceList;
        private AtomicInteger index;

        public RoundRobinSelector(List<InetSocketAddress> serviceList) {
            this.serviceList = serviceList;
            this.index = new AtomicInteger(0);
        }

        @Override
        public InetSocketAddress getNext() {
            if(serviceList == null || serviceList.isEmpty()) {
                log.error("负载均衡失败，当前没有可用的服务");
                throw new LoadBalancerException();
            }

            InetSocketAddress address = serviceList.get(index.get());

            // 如果到了最后一位，重置index
            if(index.get() == serviceList.size() - 1) {
                index.set(0);
            } else {
                // 游标后移一位
                index.incrementAndGet();
            }
            return address;
        }

        @Override
        public void reBalance() {

        }
    }
}

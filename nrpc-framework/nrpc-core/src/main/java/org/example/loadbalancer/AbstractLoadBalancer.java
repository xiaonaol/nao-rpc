package org.example.loadbalancer;

import org.example.NrpcBootstrap;
import org.example.discovery.Registry;
import org.example.exceptions.LoadBalancerException;
import org.example.loadbalancer.impl.RoundRobinLoadBalancer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xiaonaol
 * @date 2024/11/24
 **/
public abstract class AbstractLoadBalancer implements LoadBalancer {

    // 一个服务会匹配一个selector
    private Map<String, Selector> cache = new ConcurrentHashMap<>(8);

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName) {

        // 1、优先从cache中获取一个selector
        Selector selector = cache.get(serviceName);

        // 2、如果没有就需要为这个service创建一个selector
        if(selector == null) {
            // 对于这个负载均衡器，内部应该维护服务列表作为缓存
            List<InetSocketAddress> serviceList = NrpcBootstrap.getInstance().
                    getConfiguration().getRegistryConfig().getRegistry().lookup(serviceName);

            // 提供一些算法负责选取合适的节点
            selector = getSelector(serviceList);

            // 将selector放入缓存中
            cache.put(serviceName, selector);
        }

        return selector.getNext();
    }

    /**
     * 由子类进行扩展
     *
     * @param serviceList 服务列表
     * @return 负载均衡算法选择器
     */
    protected abstract Selector getSelector(List<InetSocketAddress> serviceList);

    @Override
    public synchronized void reLoadBalancer(String serviceName, List<InetSocketAddress> addresses) {
        // 根据新的服务列表生成新的selector
        cache.put(serviceName, getSelector(addresses));
    }
}

package org.example.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;

public interface Selector {
    /**
     * 根据服务列表执行一种算法获取一个服务节点
     * @return 服务地址
     */
    InetSocketAddress getNext();

    // todo 服务动态上下线需要重新进行负载均衡
    void reBalance();
}

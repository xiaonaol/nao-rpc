package org.example.discovery.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.example.Constant;
import org.example.NrpcBootstrap;
import org.example.ServiceConfig;
import org.example.discovery.AbstractRegistry;
import org.example.exceptions.DiscoveryException;
import org.example.exceptions.NetworkException;
import org.example.utils.zookeeper.NetUtils;
import org.example.utils.zookeeper.ZookeeperNode;
import org.example.utils.zookeeper.ZookeeperUtils;
import org.example.watcher.OnlineAndOfflineWatcher;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xiaonaol
 * @date 2024/10/30
 **/
@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {
    // 维护一个zk实例
    private ZooKeeper zooKeeper;

    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public ZookeeperRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString, timeout);
    }

    @Override
    public void register(ServiceConfig<?> service) {
        // 服务名称的节点
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();
        // 这个节点是一个持久节点
        if(!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建本机临时节点，ip:port
        // 服务提供方的端口一般自己设定，我们还需要一个获取ip的方法
        // ip我们通常需要一个局域网ip，不是127.0.0.1
        // todo: 后续处理端口问题
        String node = parentNode + "/" + NetUtils.getIp() + ":" + NrpcBootstrap.getInstance().getConfiguration().getPort();
        if(!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if(log.isDebugEnabled()) {
            log.debug("服务{}，已经被注册", service.getInterface().getName());
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName) {

        // 1. 找到服务对应的节点
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceName;

        // 2. 从zk中获取他的子节点
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, new OnlineAndOfflineWatcher());

        // 获取了所有的可用的服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map( ipString -> {
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.valueOf(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).toList();

        if(inetSocketAddresses.isEmpty()) {
            throw new DiscoveryException("未发现任何可用的服务主机");
        }

        return inetSocketAddresses;
    }
}

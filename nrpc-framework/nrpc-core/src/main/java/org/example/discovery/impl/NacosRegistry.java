package org.example.discovery.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.example.Constant;
import org.example.ServiceConfig;
import org.example.discovery.AbstractRegistry;
import org.example.utils.zookeeper.NetUtils;
import org.example.utils.zookeeper.ZookeeperNode;
import org.example.utils.zookeeper.ZookeeperUtils;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author xiaonaol
 * @date 2024/10/30
 **/
@Slf4j
public class NacosRegistry extends AbstractRegistry {
    // 维护一个zk实例
    private ZooKeeper zooKeeper;

    public NacosRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public NacosRegistry(String connectString, int timeout) {
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
        String node = parentNode + "/" + NetUtils.getIp() + ":" + 8088;
        if(!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        if(log.isDebugEnabled()) {
            log.debug("服务{}，已经被注册", service.getInterface().getName());
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String name) {
        return null;
    }
}

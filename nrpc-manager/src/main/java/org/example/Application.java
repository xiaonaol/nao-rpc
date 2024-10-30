package org.example;

import org.apache.zookeeper.*;
import org.example.exceptions.ZookeeperException;
import org.example.utils.zookeeper.ZookeeperNode;
import org.example.utils.zookeeper.ZookeeperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * 注册中心管理页面
 * @author xiaonaol
 * @date 2024/10/28
 **/
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws InterruptedException {

        //创建一个zookeeper实例
        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();

        // 定义节点和数据
        String basePath = "/xiaonaol-metadata";
        String providerPath = basePath + "/providers";
        String consumerPath = basePath + "/consumers";

        ZookeeperNode baseNode = new ZookeeperNode(basePath, null);
        ZookeeperNode providerNode = new ZookeeperNode(providerPath, null);
        ZookeeperNode consumerNode = new ZookeeperNode(consumerPath, null);

        List.of(baseNode, providerNode, consumerNode).forEach(node -> {
            ZookeeperUtils.createNode(zooKeeper, node, null, CreateMode.PERSISTENT);
        });

        ZookeeperUtils.close(zooKeeper);
    }
}

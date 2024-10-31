package org.example.utils.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.example.Constant;
import org.example.exceptions.ZookeeperException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author xiaonaol
 * @date 2024/10/28
 **/
@Slf4j
public class ZookeeperUtils {

    /**
     * 使用默认配置创建zookeeper实例
     * @return null
     * @author xiaonaol
     */
    public static ZooKeeper createZookeeper() {
        String connectString = Constant.DEFAULT_ZK_CONNECT;

        int timeout = Constant.TIME_OUT;

        return createZookeeper(connectString, timeout);
    }

    public static ZooKeeper createZookeeper(String connectString, int timeout) {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        try {
            // 创建Zookeeper实例，建立连接
            final ZooKeeper zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                // 只有连接成功才放行
                if(event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("Zookeeper connected");
                    countDownLatch.countDown();
                }
            });

            // 等待连接成功
            countDownLatch.await();
            return zooKeeper;
        } catch (IOException | InterruptedException e) {
            log.error("创建Zookeeper实例时发生异常: ", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 创建一个节点
     * @param zooKeeper Zookeeper实例
     * @param node 节点
     * @param watcher Watcher实例
     * @param createMode CreateMode实例
     * @return Boolean true: 成功创建 false: 已存在节点
     * @author xiaonaol
     */
    public static Boolean createNode(ZooKeeper zooKeeper, ZookeeperNode node, Watcher watcher, CreateMode createMode) {
        try {
            if(zooKeeper.exists(node.getNodePath(), watcher) == null) {
                String result = zooKeeper.create(node.getNodePath(), node.getData(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                log.info("根节点【{}】，成功创建", result);

                return true;
            } else {
                if(log.isDebugEnabled()) {
                    log.info("节点【{}】已经存在，无需创建", node.getNodePath());
                }

                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("创建基础目录时发生异常：", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 关闭zookeeper
     * @param zooKeeper
     * @author xiaonaol
     */
    public static void close(ZooKeeper zooKeeper) {
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            log.error("关闭zookeeper时发生问题：", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 判断节点是否存在
     * @param zk
     * @param node
     * @param watcher
     * @return true 存在 | false 不存在
     * @author xiaonaol
     */
    public static boolean exists(ZooKeeper zk ,String node, Watcher watcher) {
        try {
            return zk.exists(node, watcher) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("判断节点是否存在发生异常", e);
            throw new ZookeeperException(e);
        }
    }


    /**
     * 查询一个节点的子节点
     * @param zooKeeper zk实例
     * @return 子元素列表
     * @author xiaonaol
     */
    public static List<String> getChildren(ZooKeeper zooKeeper, String serviceNode, Watcher watcher) {
        try {
            return zooKeeper.getChildren(serviceNode, watcher);
        } catch (KeeperException | InterruptedException e) {
            log.error("获取节点【{}】的子节点失败", serviceNode);
            throw new ZookeeperException(e);
        }
    }
}

package com.example;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ZookeeperTest {
    ZooKeeper zooKeeper;

    @Before
    public void createZk() throws IOException {
        String connectString = "127.0.0.1:2181";
        int sessionTimeout = 5000;

        zooKeeper = new ZooKeeper(connectString, sessionTimeout, new MyWatcher());

    }

    @Test
    public void testCreatePersistentNode() throws KeeperException, InterruptedException {
        String result = zooKeeper.create("/xiaonaol", "hello".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        System.out.println("result = " + result);

        if (zooKeeper != null) {
            zooKeeper.close();
        }
    }

    @Test
    public void testWatcher() throws KeeperException, InterruptedException {
        // 以下三个方法可以注册watcher
        // 可以直接new一个新的watcher，也可以使用true来选定默认的watcher
        Stat stat = zooKeeper.exists("/xiaonaol", true);
        // zookeeper.getChildren();
        // zookeeper.getData();
        while(true) {
            Thread.sleep(10000);
        }

    }
}

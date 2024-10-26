package com.example;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class MyWatcher implements Watcher {
    @Override
    public void process(WatchedEvent event) {
        // 判断事件类型，连接类型的事件
        if (event.getType() == Watcher.Event.EventType.None) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                System.out.println("Connected to ZooKeeper");
            } else if (event.getState() == Event.KeeperState.AuthFailed) {
                System.out.println("Authentication failed");
            } else if (event.getState() == Event.KeeperState.Disconnected) {
                System.out.println("Disconnected");
            }
        } else if (event.getType() == Event.EventType.NodeCreated) {
            System.out.println("Node created");
        } else if (event.getType() == Event.EventType.NodeDataChanged) {
            System.out.println("Node data changed");
        } else if (event.getType() == Event.EventType.NodeDeleted) {
            System.out.println("Node deleted");
        }
    }
}

package org.example.watcher;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.example.netty.NettyBootstrapInitializer;
import org.example.NrpcBootstrap;
import org.example.discovery.Registry;
import org.example.loadbalancer.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * @author xiaonaol
 * @date 2024/11/30
 **/
@Slf4j
public class OnlineAndOfflineWatcher implements Watcher {

    @Override
    public void process(WatchedEvent event) {
        // 服务列表是否发生改变
        if(event.getType() == Event.EventType.NodeChildrenChanged) {
            log.info("检测到节点【{}】上/下线，将重新拉取服务列表...", event.getPath());

            String serviceName = getServiceName(event.getPath());
            Registry registry = NrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
            List<InetSocketAddress> addresses = registry.lookup(serviceName,
                    NrpcBootstrap.getInstance().getConfiguration().getGroup());
            // 处理新增的节点
            for(InetSocketAddress address : addresses) {
                // 根据地址建立连接，并且缓存
                if(!NrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    Channel channel = null;
                    try {
                        channel = NettyBootstrapInitializer.getBootstrap()
                                .connect(address).sync().channel();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    NrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            }

            // 处理下线的节点
            for(Map.Entry<InetSocketAddress, Channel> entry : NrpcBootstrap.CHANNEL_CACHE.entrySet()) {
                if(!addresses.contains(entry.getKey())) {
                    NrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                }
            }

            // 获得负载均衡器进行重新loadBalance
            LoadBalancer loadBalancer = NrpcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            loadBalancer.reLoadBalancer(serviceName, addresses);
        }
    }

    private String getServiceName(String path) {
        String[] split = path.split("/");
        return split[split.length - 1];
    }
}

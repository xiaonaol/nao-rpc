package org.example;

import io.netty.channel.Channel;
import lombok.Data;
import org.example.discovery.Registry;
import org.example.discovery.RegistryConfig;
import org.example.loadbalancer.LoadBalancer;
import org.example.loadbalancer.impl.RoundRobinLoadBalancer;
import org.example.transport.message.NrpcRequest;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局的配置类，代码配置-->xml配置-->默认项
 * @author xiaonaol
 * @date 2024/11/30
 **/
@Data
public class Configuration {
    // 配置信息-->端口号
    private int port = 8088;

    // 配置信息-->应用程序名
    private String appName = "default";

    // 配置信息-->注册中心
    private RegistryConfig registryConfig;

    // 配置信息-->序列化协议
    private ProtocolConfig protocolConfig;

    // 配置信息-->ID生成器
    private IdGenerator idGenerator = new IdGenerator(1, 2);

    // 配置信息-->序列化方式
    private String serializeType = "hessian";

    // 配置信息-->压缩方式
    private String compressType = "gzip";

    // 配置信息-->负载均衡策略
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    // 读xml
    public Configuration() {
        // 读取xml获得上面的信息

    }


    // 进行配置



}

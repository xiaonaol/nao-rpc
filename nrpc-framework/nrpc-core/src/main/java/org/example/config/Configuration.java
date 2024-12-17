package org.example.config;

import lombok.Data;

import lombok.extern.slf4j.Slf4j;
import org.example.IdGenerator;
import org.example.compress.Compressor;
import org.example.compress.impl.GzipCompressor;
import org.example.discovery.RegistryConfig;
import org.example.loadbalancer.LoadBalancer;
import org.example.loadbalancer.impl.RoundRobinLoadBalancer;
import org.example.protection.CircuitBreaker;
import org.example.protection.RateLimiter;
import org.example.serialize.Serializer;
import org.example.serialize.impl.JdkSerializer;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局的配置类，代码配置-->xml配置-->默认项
 * @author xiaonaol
 * @date 2024/11/30
 **/
@Data
@Slf4j
public class Configuration {
    // 配置信息-->端口号
    private int port = 8088;

    // 配置信息-->应用程序名
    private String appName = "default";

    // 配置信息-->注册中心
    private RegistryConfig registryConfig;

    // 配置信息-->ID生成器
    private IdGenerator idGenerator = new IdGenerator(1, 2);

    // 配置信息-->序列化方式
    private String serializeType = "hessian";
    private Serializer serializer = new JdkSerializer();

    // 配置信息-->压缩方式
    private String compressType = "gzip";
    private Compressor compressor = new GzipCompressor();

    // 配置信息-->负载均衡策略
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    // 分组信息
    private String group = "default";

    // 为每一个ip配置一个限流器
    private final Map<SocketAddress, RateLimiter> ipRateLimiter = new ConcurrentHashMap<>(16);
    // 为每一个ip配置一个断路器
    private final Map<SocketAddress, CircuitBreaker> ipCircuitBreaker = new ConcurrentHashMap<>(16);

    // 读xml
    public Configuration() {
        // 1、成员变量的默认配置项

        // 2、spi机制发现相关配置项
        SpiResolver spiResolver = new SpiResolver();
        spiResolver.loadFromSpi(this);

        // 3、读取xml获得上面的信息
        XmlResolver xmlResolver = new XmlResolver();
        xmlResolver.loadFromXml(this);

        // 4、编程配置项
    }
}

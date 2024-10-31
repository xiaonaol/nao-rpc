package org.example;

import org.example.discovery.Registry;
import org.example.discovery.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/

public class NrpcBootstrap {

    private static final Logger log = LoggerFactory.getLogger(NrpcBootstrap.class);

    // NrpcBootstrap是个单例，每个应用程序只有一个实例
    private static final NrpcBootstrap nrpcBootstrap = new NrpcBootstrap();

    // 定义相关的一些基础配置
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private int port = 8088;

    // 注册中心
    private Registry registry;

    // 维护已经发布且暴露的服务列表 key -> interface的全限定名
    private static final Map<String, ServiceConfig<?>> SERVERS_LIST = new HashMap<>(16);



    private NrpcBootstrap() {

        // 构造启动引导程序，需要做什么

    }

    public static NrpcBootstrap getInstance() {
        return nrpcBootstrap;
    }


    /**
     * 定义当前应用的名字
     * @param appName 应用名
     * @return null
     * @author xiaonaol
     */
    public NrpcBootstrap application(String appName) {
        this.appName = appName;
        return this;
    }


    /**
     * 配置一个注册中心
     * @param registryConfig 注册中心配置
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap registry(RegistryConfig registryConfig) {

        this.registry = registryConfig.getRegistry();
        return this;
    }

    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig 协议的封装
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        if(log.isDebugEnabled()) {
            log.debug("当前工程使用了： {}协议进行序列化", protocolConfig.toString());
        }
        return this;
    }

    /*
     * ----------------------------服务提供方相关api--------------------------------
     */

    /**
     * 发布服务，将接口->实现，注册到服务中心
     * @param service 封装的需要发布的服务
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap publish(ServiceConfig<?> service) {
        // 我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        registry.register(service);

        // 1.当服务调用方，通过接口、方法名、具体的方法参数列表发起调用，提供方怎么知道使用哪一个实现
        // （1）new一个 （2）spring beanFactory.getBean(class) （3）自己维护映射关系
        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    /**
     * 批量发布
     * @param services 需要发布的服务集合
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service: services) {
            this.publish(service);
        }
        return this;
    }

    /**
     * 启动netty
     * @author xiaonaol
     */
    public void start() throws InterruptedException {
        Thread.sleep(10000000);
    }


    /*
     * ----------------------------服务调用方相关api--------------------------------
     */

    public NrpcBootstrap reference(ReferenceConfig<?> reference) {
        // 在这个方法里是否可以拿到相关配置项
        // 配置reference，将来调用get方法时，方便生成代理对象
        return this;
    }
    /*
     * ----------------------------服务核心api--------------------------------
     */
}

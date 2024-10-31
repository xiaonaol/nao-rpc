package org.example;

import org.example.discovery.RegistryConfig;
import org.example.impl.HelloNrpcImpl;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
public class ProviderApplication {
    public static void main(String[] args) throws InterruptedException {
        // 服务提供方，需要注册服务，启动服务
        // 1、封装要发布的服务
        ServiceConfig<HelloNrpc> service = new ServiceConfig<>();
        service.setInterface(HelloNrpc.class);
        service.setRef(new HelloNrpcImpl());
        // 2、定义注册中心

        // 3、通过启动引导程序，启动服务提供方
        //      配置 -- 应用名称 -- 注册中心 -- 序列化协议 -- 压缩方式
        //      发布服务

        NrpcBootstrap.getInstance()
                .application("first-nrpc-provider")
                // 配置注册中心
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                // 配置协议
                .protocol(new ProtocolConfig("jdk"))
                // 发布服务
                .publish(service)
                // 启动服务
                .start();
    }
}

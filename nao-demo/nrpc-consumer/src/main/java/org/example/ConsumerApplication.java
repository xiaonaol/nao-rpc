package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.core.HeartbeatDetector;
import org.example.discovery.RegistryConfig;

import java.security.Provider;
import java.util.Map;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
@Slf4j
public class ConsumerApplication {
    public static void main(String[] args) {
        // 想尽一切办法获取代理对象，使用ReferenceConfig进行封装
        ReferenceConfig<HelloNrpc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloNrpc.class);

        // 代理做了些什么
        // 1、连接注册中心
        // 2、拉取服务列表
        // 3、选择一个服务并建立连接
        // 4、发送请求，携带一些信息（接口名，参数列表，方法名），获得结果
        NrpcBootstrap.getInstance()
                .application("first-nrpc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("hessian")
                .compress("gzip")
                .group("primary")
                .reference(reference);

        // 获取一个代理对象
        HelloNrpc helloNrpc = reference.get();
        for (int i = 0; i < 1; i++) {
            String sayHi = helloNrpc.sayHello("Hello");
            log.info("sayHi --> {}", sayHi);
        }
    }
}

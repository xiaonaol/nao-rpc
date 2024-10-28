package org.example;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
public class Application {
    public static void main(String[] args) {
        // 想尽一切办法获取代理对象，使用ReferenceConfig进行封装
        // reference一定用生成代理的模板方法.get
        ReferenceConfig<HelloNrpc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloNrpc.class);

        // 代理做了些什么
        // 1、连接注册中心
        // 2、拉去服务列表
        // 3、选择一个服务并建立连接
        // 4、发送请求，携带一些信息（接口名，参数列表，方法名），获得结果

        NrpcBootstrap.getInstance()
                .application("first-nrpc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                // 把注册中心以及调用方配置项传递给reference
                .reference(reference);

        // 获取一个代理对象
        HelloNrpc helloNrpc = reference.get();
        helloNrpc.sayHello("Hello!");
    }
}

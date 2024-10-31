package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.discovery.Registry;
import org.example.discovery.RegistryConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
@Slf4j
public class ReferenceConfig<T> {
    private Class<T> interfaceRef;

    private Registry registry;

    /**
     * 代理设计模式，生成一个api接口的代理对象
     * @author 代理对象
     */
    public T get() {
        // 此处是使用动态代理完成了一些工作
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceRef};

        // 使用动态代理生成代理对象
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 调用sayHi方法，会走进这个代码段中
                // 已知method，args
                log.info("method-->{}", method.getName());
                log.info("args-->{}", args);

                // 传入服务的名字，返回ip+端口
                // todo 我们每次调用相关方法的时候都需要去注册中心拉取服务列表吗？ 本地缓存 + watcher
                // todo 我们如何合理地选择一个可用的服务而不是只获取第一个
                InetSocketAddress address = registry.lookup(interfaceRef.getName());
                if(log.isDebugEnabled()) {
                    log.debug("服务调用方发现了服务【{}】的可用主机【{}】", interfaceRef.getName(),address);
                }
                // 使用netty连接服务器，发送 服务名+方法名+参数列表，得到结果

                return null;
            }
        });

        return (T)helloProxy;
    }

    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Registry getRegistry() {
        return registry;
    }
}

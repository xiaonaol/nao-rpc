package org.example;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
public class ReferenceConfig<T> {
    private Class<T> interfaceRef;

    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }


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
                System.out.println("hello proxy");
                return null;
            }
        });

        return (T)helloProxy;
    }
}

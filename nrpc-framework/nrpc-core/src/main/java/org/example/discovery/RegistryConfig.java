package org.example.discovery;

import org.example.Constant;
import org.example.exceptions.DiscoveryException;
import org.example.impl.NacosRegistry;
import org.example.impl.ZookeeperRegistry;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
public class RegistryConfig {
    // 定义连接的url
    private String connectString;

    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }
    
    
    /**
     * 使用简单工厂实现
     * @return registry 返回具体注册中心实例
     * @author xiaonaol
     */
    public Registry getRegistry() {
        // 1.获取注册中心的类型
        String registryType = getRegistryType(connectString, true).toLowerCase().trim();
        if(registryType.equals("zookeeper")) {
            String host = getRegistryType(connectString, false).toLowerCase().trim();
            return new ZookeeperRegistry(host, Constant.TIME_OUT);
        } else if(registryType.equals("nacos")) {
            String host = getRegistryType(connectString, false).toLowerCase().trim();
            return new NacosRegistry(host, Constant.TIME_OUT);
        }
        throw new DiscoveryException("未发现合适注册中心");
    }

    private String getRegistryType(String connectString, Boolean ifType){
        String[] typeAndHost = connectString.split("://");
        if(typeAndHost.length != 2) {
            throw new RuntimeException("给定的注册中心连接url不合法: " + connectString);
        }
        if(ifType) {
            return typeAndHost[0];
        } else {
            return typeAndHost[1];
        }
    }
}

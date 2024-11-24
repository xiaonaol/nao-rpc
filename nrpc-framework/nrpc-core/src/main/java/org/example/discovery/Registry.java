package org.example.discovery;

import org.example.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 注册中心
 * @author xiaonaol
 * @date 2024/10/30
 **/
public interface Registry {

    /**
     * 注册服务
     * @param serviceConfig 服务配置内容
     * @author xiaonaol
     */
    void register(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取一个可用的服务
     * @param serviceName 服务名
     * @return 服务的地址
     * @author xiaonaol
     */
    List<InetSocketAddress> lookup(String serviceName);
}

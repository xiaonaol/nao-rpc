package org.example.discovery;

import org.example.ServiceConfig;

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
}

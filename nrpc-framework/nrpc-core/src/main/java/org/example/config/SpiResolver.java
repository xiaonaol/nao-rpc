package org.example.config;

import org.example.loadbalancer.LoadBalancer;
import org.example.spi.SpiHandler;

/**
 * @author xiaonaol
 * @date 2024/12/1
 **/
public class SpiResolver {
    
    /**
     * 通过spi的方式加载配置项
     * @param configuration 配置上下文
     * @author xiaonaol
     */
    public void loadFromSpi(Configuration configuration) {

        // 1
        LoadBalancer loadBalancer = SpiHandler.get(LoadBalancer.class);
        configuration.setLoadBalancer(loadBalancer);
    }
}

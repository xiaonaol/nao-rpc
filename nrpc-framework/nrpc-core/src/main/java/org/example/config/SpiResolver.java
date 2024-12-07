package org.example.config;

import org.example.compress.Compressor;
import org.example.compress.CompressorFactory;
import org.example.loadbalancer.LoadBalancer;
import org.example.serialize.Serializer;
import org.example.serialize.SerializerFactory;
import org.example.spi.SpiHandler;

import javax.swing.*;
import java.util.List;

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

        // spi文件中配置了很多实现
        List<ObjectWrapper<LoadBalancer>> loadBalancerWrappers = SpiHandler.getList(LoadBalancer.class);
        // 将其放入工厂
        if(loadBalancerWrappers != null && !loadBalancerWrappers.isEmpty()) {
            configuration.setLoadBalancer(loadBalancerWrappers.get(0).getImpl());
        }

        List<ObjectWrapper<Compressor>> compressorWrappers = SpiHandler.getList(Compressor.class);
        if(compressorWrappers != null) {
            compressorWrappers.forEach(CompressorFactory::addCompressor);
        }

        List<ObjectWrapper<Serializer>> serializerWrappers = SpiHandler.getList(Serializer.class);
        if(serializerWrappers != null) {
            serializerWrappers.forEach(SerializerFactory::addSerializer);
        }
    }
}

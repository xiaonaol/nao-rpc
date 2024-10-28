package org.example;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/

public class NrpcBootstrap {

    private static final Logger log = LoggerFactory.getLogger(NrpcBootstrap.class);

    // NrpcBootstrap是个单例，每个应用程序只有一个实例
    private static NrpcBootstrap nrpcBootstrap = new NrpcBootstrap();

    private NrpcBootstrap() {
        // 构造启动引导程序，需要做什么

    }

    public static NrpcBootstrap getInstance() {
        return nrpcBootstrap;
    }


    /**
     * 定义当前应用的名字
     * @param appName
     * @return null
     * @author xiaonaol
     */
    public NrpcBootstrap application(String appName) {
        return this;
    }


    /**
     * 配置一个注册中心
     * @param registryConfig
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap registry(RegistryConfig registryConfig) {
        return this;
    }

    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig 协议的封装
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        if(log.isDebugEnabled()) {
            log.debug("当前工程使用了： {}协议进行序列化", protocolConfig.toString());
        }
        return this;
    }

    /**
     * ----------------------------服务提供方相关api--------------------------------
     */

    /**
     * 发布服务，将接口->实现，注册到服务中心
     * @param service 封装的需要发布的服务
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap publish(ServiceConfig<?> service) {
        if(log.isDebugEnabled()) {
            log.debug("服务{}，已经被注册", service.getInterface().getName());
        }
        return this;
    }

    /**
     * 批量发布
     * @param services 需要发布的服务集合
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap publish(List<?> services) {
        return this;
    }

    /**
     * 启动netty
     * @param
     * @return null
     * @author xiaonaol
     */
    public NrpcBootstrap start() {
        return this;
    }


    /**
     * ----------------------------服务调用方相关api--------------------------------
     */

    public NrpcBootstrap reference(ReferenceConfig<?> reference) {
        // 在这个方法里是否可以拿到相关配置项
        // 配置reference，将来调用get方法时，方便生成代理对象
        return this;
    }
    /**
     * ----------------------------服务核心api--------------------------------
     */
}

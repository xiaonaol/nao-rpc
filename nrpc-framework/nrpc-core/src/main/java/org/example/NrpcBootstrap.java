package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.example.annotation.NrpcApi;
import org.example.config.Configuration;
import org.example.core.HeartbeatDetector;
import org.example.discovery.RegistryConfig;
import org.example.loadbalancer.LoadBalancer;
import org.example.netty.initializer.NettyServerBootstrapInitializer;
import org.example.transport.message.NrpcRequest;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author xiaonaol
 * @date 2024/10/27
 **/
@Slf4j
public class NrpcBootstrap {

    private final Configuration configuration;

    // NrpcBootstrap是个单例，每个应用程序只有一个实例
    private static final NrpcBootstrap nrpcBootstrap = new NrpcBootstrap();

    // 保存request对象，可以在当前线程中随时获取
    public static final ThreadLocal<NrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    // 维护已经发布且暴露的服务列表 key -> interface的全限定名
    public static final Map<String, ServiceConfig<?>> SERVERS_LIST = new HashMap<>(16);

    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    public static final TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    // 定义全局的对外挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_QUEST = new ConcurrentHashMap<>(128);

    private NrpcBootstrap() {
        // 构造启动引导程序，需要做什么
        configuration = new Configuration();
    }

    public static NrpcBootstrap getInstance() {
        return nrpcBootstrap;
    }


    /**
     * 定义当前应用的名字
     * @param appName 应用名
     * @return null
     * @author xiaonaol
     */
    public NrpcBootstrap application(String appName) {
        configuration.setAppName(appName);
        return this;
    }


    /**
     * 配置一个注册中心
     * @param registryConfig 注册中心配置
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap registry(RegistryConfig registryConfig) {
        configuration.setRegistryConfig(registryConfig);
        return this;
    }

    /**
     * 配置负载均衡策略
     * @param loadBalancer 注册中心配置
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
        return this;
    }

    /*
     * ----------------------------服务提供方相关api--------------------------------
     */

    /**
     * 发布服务，将接口->实现，注册到服务中心
     *
     * @param service 封装的需要发布的服务
     * @author xiaonaol
     */
    public void publish(ServiceConfig<?> service) {
        // 我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        configuration.getRegistryConfig().getRegistry().register(service);

        SERVERS_LIST.put(service.getInterface().getName(), service);
    }

    /**
     * 批量发布
     * @param services 需要发布的服务集合
     * @return this当前实例
     * @author xiaonaol
     */
    public NrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service: services) {
            this.publish(service);
        }
        return this;
    }

    /**
     * 启动netty
     * @author xiaonaol
     */
    public void start() {
        try {
            // 配置Netty服务器
            ServerBootstrap serverBootstrap = NettyServerBootstrapInitializer.getServerBootstrap();

            // 绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(configuration.getPort()).sync();

            // 阻塞等待应用关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            log.error("服务提供方运行过程中出现错误", e);
        }
    }


    /*
     * ----------------------------服务调用方相关api--------------------------------
     */

    public void reference(ReferenceConfig<?> reference) {

        // 开启对这个服务的心跳检测
        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());

        // 在这个方法里是否可以拿到相关配置项
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、reference需要一个注册中心
        reference.setRegistry(configuration.getRegistryConfig().getRegistry());
        reference.setGroup(this.getConfiguration().getGroup());
    }

    /**
     * 配置序列化方式
     * @param serializeType 序列化方式
     */
    public NrpcBootstrap serialize(String serializeType) {
        configuration.setSerializeType(serializeType);
        return this;
    }

    /**
     * 配置压缩类型
     * @param compressType 压缩类型
     */
    public NrpcBootstrap compress(String compressType) {
        configuration.setCompressType(compressType);
        return this;
    }


    /**
     * 扫包批量发布服务
     * @param packageName 项目包名
     * @return NrpcBootstrap
     * @author xiaonaol
     */
    public NrpcBootstrap scan(String packageName) {
        // 需要通过packageName获取其下所有类的全限定名称
        List<String> classNames = getAllClassNames(packageName);

        // 2、通过反射获取他的接口，构建具体实现
        List<Class<?>> classes = classNames.stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(clazz -> clazz.getAnnotation(NrpcApi.class) != null)
                .collect(Collectors.toList());

        for(Class<?> clazz : classes) {
            // 获取他的接口
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (NoSuchMethodException | InvocationTargetException |
                     InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // 获取分组信息
            NrpcApi nrpcApi = clazz.getAnnotation(NrpcApi.class);
            String group = nrpcApi.group();

            for(Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);
                serviceConfig.setGroup(group);

                if (log.isDebugEnabled()) {
                    log.debug("已经通过包扫描，将服务【{}】发布", anInterface);
                }
                // 3、发布
                publish(serviceConfig);
            }
        }

        return this;
    }

    private List<String> getAllClassNames(String packageName) {
        // 1、通过packageName获得绝对路径
        // com.example.xxx.yyy -> E://...
        String basePath = packageName.replaceAll("\\.", "/");
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);
        if(url == null) {
            throw new RuntimeException("包扫描时路径不存在");
        }
        String absolutePath = url.getPath();
        //
        List<String> classNames = new ArrayList<>();
        classNames = recursionFile(absolutePath, classNames, basePath);

        return classNames;
    }

    private List<String> recursionFile(String absolutePath, List<String> classNames, String basePath) {
        // 获取文件
        File file = new File(absolutePath);
        // 判断文件是否是文件夹
        if(file.isDirectory()) {
            // 找到文件夹的所有文件
            File[] children = file.listFiles(pathname -> pathname.isDirectory() || pathname.getPath().contains(".class"));
            if(children == null) {
                return classNames;
            }
            for(File child: children) {
                if(child.isDirectory()) {
                    // 递归调用
                    recursionFile(child.getAbsolutePath(), classNames, basePath);
                } else {
                    // 文件 --> 类的全限定名称
                    String className = getClassNameByAbsolutePath(child.getAbsolutePath(), basePath);
                    classNames.add(className);
                }
            }
        } else {
            // 文件 --> 类的全限定名称
            String className = getClassNameByAbsolutePath(absolutePath, basePath);
            classNames.add(className);
        }
        return classNames;
    }

    private String getClassNameByAbsolutePath(String absolutePath, String basePath) {
        String fileName = absolutePath.
                substring(absolutePath.indexOf(basePath.replaceAll("/", "\\\\")))
                .replaceAll("\\\\", ".");

        fileName = fileName.substring(0, fileName.indexOf(".class"));

        return fileName;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public NrpcBootstrap group(String group) {
        this.getConfiguration().setGroup(group);
        return this;
    }

}

package org.example.spi;

import jdk.jshell.spi.SPIResolutionException;
import lombok.extern.slf4j.Slf4j;
import org.example.config.ObjectWrapper;
import org.example.exceptions.SpiException;
import org.example.loadbalancer.LoadBalancer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 实现一个简易版的spi
 * @author xiaonaol
 * @date 2024/12/2
 **/
@Slf4j
public class SpiHandler {

    // 定义一个basePath
    private static final String BASE_PATH = "META-INF/services/";

    // 先定义一个缓存，保存spi相关的原始内容
    private static final Map<String, List<String>> SPI_CONTENT = new ConcurrentHashMap<>(8);

    // 缓存每一个接口所对应的实现的实例
    private static final Map<Class<?>, List<ObjectWrapper<?>>> SPI_IMPL = new ConcurrentHashMap<>(32);

    static {
        // 如何加载当前工程和jar包中的classPath中的资源
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL fileUrl = classLoader.getResource(BASE_PATH);
        if (fileUrl != null) {
            File file = new File(fileUrl.getFile());
            File[] children = file.listFiles();
            if(children != null) {
                for (File child : children) {
                    String key = child.getName();
                    List<String> value = getImplNames(child);
                    SPI_CONTENT.put(key, value);
                }
            }
        }
    }

    private static List<String> getImplNames(File child) {
        try (FileReader reader = new FileReader(child);
             BufferedReader bufferedReader = new BufferedReader(reader)
        ) {
            List<String> implNames = new ArrayList<>();
            while(true) {
                String line = bufferedReader.readLine();
                if (line == null || line.isEmpty()) break;
                implNames.add(line);
            }
            return implNames;
        } catch (IOException e) {
            log.error("读取spi文件时发生异常", e);
        }

        return null;
    }
    
    
    /**
     * 获取一个和当前服务相关的实例
     * @param clazz 一个类的class实例
     * @return      实力类的实现
     * @author xiaonaol
     */
    public static <T> ObjectWrapper<T> get(Class<T> clazz) {

        // 1、优先走缓存
        List<ObjectWrapper<?>> objectWrappers = SPI_IMPL.get(clazz);
        if(objectWrappers != null && !objectWrappers.isEmpty()) {
            return (ObjectWrapper<T>) objectWrappers.get(0);
        }

        // 2、构建缓存
        buildCache(clazz);

        List<ObjectWrapper<?>> result = SPI_IMPL.get(clazz);
        if(result == null) {
            return null;
        }

        // 3、再次尝试获取第一个
        return (ObjectWrapper<T>) SPI_IMPL.get(clazz).get(0);
    }


    /**
     * 获取所有和当前服务相关的实例
     * @param clazz 一个服务接口的class实例
     * @return      实现类的实例集合
     * @author xiaonaol
     */
    public synchronized static <T> List<ObjectWrapper<T>> getList(Class<T> clazz) {

        // 1、优先走缓存
        List<ObjectWrapper<?>> objectWrappers = SPI_IMPL.get(clazz);
        if(objectWrappers != null && !objectWrappers.isEmpty()) {
            return objectWrappers.stream().map(wrapper -> (ObjectWrapper<T>) wrapper)
                    .collect(Collectors.toList());
        }

        // 2、建立缓存
        buildCache(clazz);

        // 3、再次获取
        objectWrappers = SPI_IMPL.get(clazz);
        return objectWrappers.stream().map(wrapper -> (ObjectWrapper<T>) wrapper)
                .collect(Collectors.toList());
    }


    /**
     * 构建clazz相关的缓存
     * @param clazz 一个类的class实例
     * @author xiaonaol
     */
    private static void buildCache(Class<?> clazz) {

        // 1、通过clazz获取与之匹配的实现名称
        String name = clazz.getName();
        List<String> implNames = SPI_CONTENT.get(name);
        if(implNames == null) {
            return;
        }

        // 2、实例化所有的实现
        List<ObjectWrapper<?>> impls = new ArrayList<>();
        for(String implName : implNames) {
            try {
                // 首先进行分割
                String[] codeAndTypeAndName = implName.split("-");
                if(codeAndTypeAndName.length != 3) {
                    throw new SpiException("配置的spi文件不合法");
                }
                Byte code = Byte.parseByte(codeAndTypeAndName[0]);
                String type = codeAndTypeAndName[1];
                String implementName = codeAndTypeAndName[2];

                Class<?> aClass = Class.forName(implementName);
                Object impl = aClass.getConstructor().newInstance();

                ObjectWrapper<?> objectWrapper = new ObjectWrapper<>(code, type, implementName);

                impls.add(objectWrapper);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     InstantiationException | IllegalAccessException e) {
                log.error("实例化【{}】的实现时发生了异常", implName, e);
            }
        }
        SPI_IMPL.put(clazz, impls);
    }
}

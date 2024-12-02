package org.example.spi;

import lombok.extern.slf4j.Slf4j;
import org.example.loadbalancer.LoadBalancer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<Class<?>, List<Object>> SPI_IMPL = new ConcurrentHashMap<>(32);

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
    public static <T> T get(Class<T> clazz) {

        // 1、优先走缓存
        List<Object> impls= SPI_IMPL.get(clazz);
        if(impls != null && !impls.isEmpty()) {
            return (T) impls.get(0);
        }

        // 2、构建缓存
        buildCache(clazz);

        // 3、再次尝试获取第一个
        return (T) SPI_IMPL.get(clazz).get(0);
    }


    /**
     * 获取所有和当前服务相关的实例
     * @param clazz 一个服务接口的class实例
     * @return      实现类的实例集合
     * @author xiaonaol
     */
    public static <T> List<T> getList(Class<T> clazz) {

        // 1、优先走缓存
        List<T> impls = (List<T>) SPI_IMPL.get(clazz);
        if(impls != null && !impls.isEmpty()) {
            return impls;
        }

        // 2、建立缓存
        buildCache(clazz);

        // 3、获取所有和当前
        return (List<T>) SPI_IMPL.get(clazz);
    }


    /**
     * 构建clazz相关的缓存
     * @param clazz 一个类的class实例
     * @return null
     * @author xiaonaol
     */
    private static void buildCache(Class<?> clazz) {

        // 1、通过clazz获取与之匹配的实现名称
        String name = clazz.getName();
        List<String> implNames = SPI_CONTENT.get(name);

        // 2、实例化所有的实现
        List<Object> impls = new ArrayList<>();
        for(String implName : implNames) {
            try {
                Class<?> aClass = Class.forName(implName);
                Object impl = aClass.getConstructor().newInstance();
                impls.add(impl);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     InstantiationException | IllegalAccessException e) {
                log.error("实例化【{}】的实现时发生了异常", implName, e);
            }
        }
        SPI_IMPL.put(clazz, impls);
    }
}

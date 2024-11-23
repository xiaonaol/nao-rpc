package org.example.serialize;

import lombok.extern.slf4j.Slf4j;
import org.example.serialize.impl.HessianSerializer;
import org.example.serialize.impl.JdkSerializer;
import org.example.serialize.impl.JsonSerializer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
@Slf4j
public class SerializerFactory {
    private final static ConcurrentHashMap<String, SerializerWrapper> SERIALIZER_CACHE = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Byte, SerializerWrapper> SERIALIZER_CACHE_CODE = new ConcurrentHashMap<>();
    static {
        SerializerWrapper jdk = new SerializerWrapper((byte) 1, "jdk", new JdkSerializer());
        SerializerWrapper json = new SerializerWrapper((byte) 2, "json", new JsonSerializer());
        SerializerWrapper hessian = new SerializerWrapper((byte) 3, "hessian", new HessianSerializer());

        SERIALIZER_CACHE.put("jdk", jdk);
        SERIALIZER_CACHE.put("json", json);
        SERIALIZER_CACHE.put("hessian", hessian);

        SERIALIZER_CACHE_CODE.put((byte) 1, jdk);
        SERIALIZER_CACHE_CODE.put((byte) 2, json);
        SERIALIZER_CACHE_CODE.put((byte) 3, hessian);
    }

    /**
     * 使用工厂方法获取SerializerWrapper
     * @param serializeType 序列化类型
     * @return 序列化包装类
     */
    public static SerializerWrapper getSerializer(String serializeType) {
        SerializerWrapper serializerWrapper = SERIALIZER_CACHE.get(serializeType);
        if(serializerWrapper == null) {
            log.info("获取序列化类型失败，使用默认序列化方式hessian");
            return SERIALIZER_CACHE.get("hessian");
        }
        return serializerWrapper;
    }

    public static SerializerWrapper getSerializer(byte serializeCode) {
        SerializerWrapper serializerWrapper = SERIALIZER_CACHE_CODE.get(serializeCode);
        if(serializerWrapper == null) {
            log.info("获取序列化类型失败，使用默认序列化方式hessian");
            return SERIALIZER_CACHE_CODE.get((byte) 3);
        }
        return serializerWrapper;
    }
}

package org.example.compress;

import org.example.compress.impl.GzipCompressor;
import org.example.config.ObjectWrapper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiaonaol
 * @date 2024/11/23
 **/
public class CompressorFactory {
    private final static ConcurrentHashMap<String, ObjectWrapper<Compressor>> COMPRESSOR_CACHE = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Byte, ObjectWrapper<Compressor>> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>();

    static {
        ObjectWrapper<Compressor> gzip = new ObjectWrapper<>((byte) 1, "gzip", new GzipCompressor());
        COMPRESSOR_CACHE.put("gzip", gzip);
        COMPRESSOR_CACHE_CODE.put((byte) 1, gzip);
    }

    /**
     * 使用工厂方法获取CompressorWrapper
     * @param compressType 序列化类型
     * @return 序列化包装类
     */
    public static ObjectWrapper<Compressor> getCompressor(String compressType) {
        ObjectWrapper<Compressor> compressorWrapper = COMPRESSOR_CACHE.get(compressType);
        if(compressorWrapper == null) {
            return COMPRESSOR_CACHE.get("gzip");
        }
        return compressorWrapper;
    }

    public static ObjectWrapper<Compressor> getCompressor(byte compressCode) {
        ObjectWrapper<Compressor> compressorWrapper = COMPRESSOR_CACHE.get(compressCode);
        if(compressorWrapper == null) {
            return COMPRESSOR_CACHE_CODE.get((byte) 1);
        }
        return compressorWrapper;
    }


    /**
     * 添加一个新的压缩策略
     * @param compressorWrapper    压缩类型的包装
     * @author xiaonaol
     */
    public static void addCompressor(ObjectWrapper<Compressor> compressorWrapper) {
        COMPRESSOR_CACHE.put(compressorWrapper.getName(), compressorWrapper);
        COMPRESSOR_CACHE_CODE.put(compressorWrapper.getCode(), compressorWrapper);
    }
}

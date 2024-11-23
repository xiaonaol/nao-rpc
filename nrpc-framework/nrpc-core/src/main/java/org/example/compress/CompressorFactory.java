package org.example.compress;

import org.example.compress.impl.GzipCompressor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiaonaol
 * @date 2024/11/23
 **/
public class CompressorFactory {
    private final static ConcurrentHashMap<String, CompressorWrapper> COMPRESSOR_CACHE = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Byte, CompressorWrapper> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>();

    static {
        CompressorWrapper gzip = new CompressorWrapper((byte) 1, "gzip", new GzipCompressor());
        COMPRESSOR_CACHE.put("gzip", gzip);
        COMPRESSOR_CACHE_CODE.put((byte) 1, gzip);
    }

    /**
     * 使用工厂方法获取CompressorWrapper
     * @param compressType 序列化类型
     * @return 序列化包装类
     */
    public static CompressorWrapper getCompressor(String compressType) {
        CompressorWrapper compressorWrapper = COMPRESSOR_CACHE.get(compressType);
        if(compressorWrapper == null) {
            return COMPRESSOR_CACHE.get("gzip");
        }
        return compressorWrapper;
    }

    public static CompressorWrapper getCompressor(byte compressCode) {
        CompressorWrapper compressorWrapper = COMPRESSOR_CACHE.get(compressCode);
        if(compressorWrapper == null) {
            return COMPRESSOR_CACHE_CODE.get((byte) 1);
        }
        return compressorWrapper;
    }
}

package org.example.compress.impl;

import ch.qos.logback.core.pattern.color.BoldYellowCompositeConverter;
import lombok.extern.slf4j.Slf4j;
import org.example.compress.Compressor;
import org.example.exceptions.CompressException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 用gzip算法进行解压缩的具体实现
 * @author xiaonaol
 * @date 2024/11/23
 **/
@Slf4j
public class GzipCompressor implements Compressor {
    @Override
    public byte[] compress(byte[] bytes) {

        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
        ) {
            gzip.write(bytes);
            gzip.finish();

            byte[] result = baos.toByteArray();
            if (log.isDebugEnabled()) {
                log.debug("对报文进行压缩长度由【{}】压缩至【{}】", bytes.length, result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("报文压缩时发生异常", e);
            throw new CompressException(e);
        }
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        log.info("【{}】", bytes.length);
        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            GZIPInputStream gzip = new GZIPInputStream(bais);
        ) {
            byte[] result = gzip.readAllBytes();
            if(log.isDebugEnabled()) {
                log.error("对报文进行了解压长度由【{}】变为【{}】", bytes.length, result.length);
            }

            return result;
        } catch (IOException e) {
            log.error("报文解压时发生异常", e);
            throw new CompressException(e);
        }
    }
}

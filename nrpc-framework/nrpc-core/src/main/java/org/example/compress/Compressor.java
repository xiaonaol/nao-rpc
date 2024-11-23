package org.example.compress;

/**
 * @author xiaonaol
 * @date 2024/11/23
 **/
public interface Compressor {
    byte[] compress(byte[] data);
    byte[] decompress(byte[] data);
}

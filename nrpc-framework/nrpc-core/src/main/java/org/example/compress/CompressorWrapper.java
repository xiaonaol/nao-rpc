package org.example.compress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.compress.impl.GzipCompressor;

/**
 * @author xiaonaol
 * @date 2024/11/23
 **/
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CompressorWrapper {
    private byte code;
    private String type;
    private Compressor compressor;

}

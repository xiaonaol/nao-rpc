package org.example.serialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SerializerWrapper {
    private byte code;
    private String type;
    private Serializer serializer;
}

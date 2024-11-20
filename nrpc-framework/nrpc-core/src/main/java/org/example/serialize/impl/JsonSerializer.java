package org.example.serialize.impl;

import org.example.serialize.Serializer;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        return null;
    }
}

package org.example.serialize.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.exceptions.SerializeException;
import org.example.serialize.Serializer;

import java.io.*;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
@Slf4j
public class JdkSerializer implements Serializer {

    @Override
    public byte[] serialize(Object object) {
        if(object == null) {
            return null;
        }
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
        ) {
            oos.writeObject(object);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("jdk序列化对象【{}】出现异常", object);
            throw new SerializeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes == null || clazz == null) {
            return null;
        }

        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
        ) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("jdk反序列化对象【{}】出现异常", clazz);
            throw new SerializeException(e);
        }
    }
}

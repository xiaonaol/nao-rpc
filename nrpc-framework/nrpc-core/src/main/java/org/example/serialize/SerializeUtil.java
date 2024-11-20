package org.example.serialize;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
@Slf4j
public class SerializeUtil {
    public static byte[] serialize(Object object) {
        if(object == null) {
            return null;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
            outputStream.writeObject(object);

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error("序列化出现异常");
            throw new RuntimeException(e);
        }
    }
}

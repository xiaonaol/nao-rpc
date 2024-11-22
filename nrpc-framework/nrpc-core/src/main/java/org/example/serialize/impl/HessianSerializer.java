package org.example.serialize.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import lombok.extern.slf4j.Slf4j;
import org.example.exceptions.SerializeException;
import org.example.serialize.Serializer;

import java.io.*;

/**
 * @author xiaonaol
 * @date 2024/11/23
 **/
@Slf4j
public class HessianSerializer implements Serializer {

    @Override
    public byte[] serialize(Object object) {
        if(object == null) {
            return null;
        }
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ) {
            log.info("使用hessian序列化");
            Hessian2Output hessian2Output = new Hessian2Output(baos);
            hessian2Output.writeObject(object);
            hessian2Output.flush();
            if(log.isDebugEnabled()) {
                log.debug("对象使用hessian【{}】已经完成了序列化", object);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("hessian序列化对象【{}】出现异常", object);
            throw new SerializeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes == null || clazz == null) {
            return null;
        }

        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ) {
            log.info("使用hessian反序列化");
            Hessian2Input hessianInput = new Hessian2Input(bais);
            T t = (T) hessianInput.readObject();
            if(log.isDebugEnabled()) {
                log.debug("类【{}】已经使用hessian完成了反序列化操作", clazz);
            }
            return t;
        } catch (IOException e) {
            log.error("jdk反序列化对象【{}】出现异常", clazz);
            throw new SerializeException(e);
        }
    }
}

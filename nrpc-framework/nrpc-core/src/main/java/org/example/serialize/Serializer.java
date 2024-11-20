package org.example.serialize;

/**
 * 序列化器
 * @author xiaonaol
 * @date 2024/11/20
 **/
public interface Serializer {
    /**
     * 抽象的用来做序列化的方法
     * @param object 待序列化的对象
     * @return 字节数组
     */
    byte[] serialize(Object object);

    /**
     * 反序列化的方法
     * @param bytes 待
     * @param clazz 目标类的class对象
     * @return 目标实例
     * @param <T> 目标泛型
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}

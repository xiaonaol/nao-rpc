package org.example.transport.message;

/**
 * 请求调用方所请求的接口方法的描述
 * @author xiaonaol
 * @date 2024/11/4
 **/
public class RequestPayload {
    // 接口名 -- org.example.HelloNrpc
    private String interfaceName;

    // 方法名 -- sayHi
    private String methodName;

    // 参数列表
    // 参数类型用来确定重载方法，具体的参数用来执行方法调用
    private Class<?>[] parametersType; // -- {java.long.String}
    private Object[] parametersValue; // -- "你好"

    // 返回值的封装 -- {java.long.String}
    private Class<?> returnType;

}

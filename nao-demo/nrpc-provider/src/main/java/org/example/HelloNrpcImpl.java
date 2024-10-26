package org.example;

/**
 * Implement HelloNrpc Interface
 *
 * @author xiaonaol
 * @date 2024/10/26
 **/
public class HelloNrpcImpl implements HelloNrpc{
    @Override
    public String sayHello(String msg) {
        return "hi consumer: " + msg;
    }
}

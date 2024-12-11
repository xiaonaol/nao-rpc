package org.example.impl;

import org.example.HelloNrpc;
import org.example.HelloNrpc2;
import org.example.annotation.NrpcApi;

/**
 * Implement HelloNrpc Interface
 *
 * @author xiaonaol
 * @date 2024/10/26
 **/
@NrpcApi(group = "primary")
public class HelloNrpc2Impl implements HelloNrpc2 {
    @Override
    public String sayHello(String msg) {
        return "hi consumer: " + msg;
    }
}

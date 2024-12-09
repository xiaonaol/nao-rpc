package org.example;

import org.example.annotation.TryTimes;

public interface HelloNrpc {


    /**
     * @param msg
     * @return String
     * @author xiaonaol
     */
    @TryTimes(tryTimes = 3, intervalTime = 3000)
    String sayHello(String msg);
}

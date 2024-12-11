package org.example.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author xiaonaol
 * @date 2024/12/11
 **/
public class ShutdownHolder {

    // 请求挡板
    public static AtomicBoolean BAFFLE = new AtomicBoolean(false);

    // 请求计数器
    public static LongAdder LATCH = new LongAdder();
}

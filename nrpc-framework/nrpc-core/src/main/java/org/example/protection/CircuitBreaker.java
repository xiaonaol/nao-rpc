package org.example.protection;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单熔断器的实现
 * @author xiaonaol
 * @date 2024/12/8
 **/
public class CircuitBreaker {

    // 标准的断路器应该有三种状态 open close half-open
    private volatile boolean isOpen = false;

    // 需要搜集异常指标
    // 异常的数量 总的请求数
    private AtomicInteger requestCount = new AtomicInteger(0);

    // 异常的请求数
    private AtomicInteger errorRequestCount = new AtomicInteger(0);

    // 允许最大的异常比例
    private int maxErrorRequest;
    private float maxErrorRate;

    public CircuitBreaker(int maxErrorRequest, float maxErrorRate) {
        this.maxErrorRequest = maxErrorRequest;
        this.maxErrorRate = maxErrorRate;
    }

    // 断路器的核心方法，判断是否开启
    public boolean isBreak() {
        if(isOpen) {
            return true;
        }

        // 需要判断数据指标，是否满足当前的熔断阈值
        if(errorRequestCount.get() > maxErrorRequest) {
            this.isOpen = true;
            return true;
        }

        if(errorRequestCount.get() > 0 && requestCount.get() > 0 &&
        errorRequestCount.get() / (float) requestCount.get() > maxErrorRate) {
            this.isOpen = true;
            return true;
        }

        return false;
    }



    // 每次发生请求、异常应该进行记录
    public void recordRequest() {
        this.requestCount.getAndIncrement();
    }

    public void recordErrorRequest() {
        this.errorRequestCount.getAndIncrement();
    }


    /**
     * 重置熔断器
     * @author xiaonaol
     */
    public void reset() {
        this.isOpen = false;
        this.requestCount.set(0);
        this.errorRequestCount.set(0);
    }
}

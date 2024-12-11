package org.example.protection;

/**
 * 基于令牌桶算法的限流器
 * @author xiaonaol
 * @date 2024/12/8
 **/
public class TokenBuketRateLimiter implements RateLimiter {

    // 代表令牌数量
    private int tokens;

    // 限流的本质就是 令牌数
    private final int capacity;

    // 按照一定的速率给令牌桶加令牌
    private final int rate;

    // 上一次放令牌的时间戳
    private Long lastTokenTime;

    public TokenBuketRateLimiter(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        lastTokenTime = System.currentTimeMillis();
        tokens = capacity;
    }

    
    /**
     * @return true 放行 false 拦截
     * @author xiaonaol
     */
    public synchronized boolean allowRequest() {
        // 1、给令牌桶添加令牌
        Long currentTime = System.currentTimeMillis();
        Long timeInterval = currentTime - lastTokenTime;
        if(timeInterval >= 1000 / rate) {
            int needAddToken = (int) (timeInterval * rate / 1000);

            // 给令牌桶添加令牌
            tokens = Math.min(capacity, tokens + needAddToken);

            // 标记最后一次放入令牌的时间
            this.lastTokenTime = System.currentTimeMillis();
        }

        // 2、自己获取令牌，如果令牌桶中有令牌则放行，否则拦截
        if(tokens > 0) {
            tokens --;
            return true;
        } else {
            return false;
        }
    }
}

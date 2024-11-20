package org.example;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author xiaonaol
 * @date 2024/11/20
 **/
public class IdGenerator {
    private static final LongAdder longAdder = new LongAdder();

    // 雪花算法
    // 机房号 5bit 32
    // 机器号 5bit
    // 时间戳(long 1970-1-1) 原本64位表示的时间必须减少到42位，自由选择一个比较近的时间
    // 序列号 12bit

    // 起始时间戳
    public static final long START_STAMP = DateUtil.get("2022-1-1").getTime();
    // 定义常量
    public static final long DATA_CENTER_BIT = 5L;
    public static final long MACHINE_BIT = 5L;
    public static final long SEQUENCE_BIT = 12L;
    // 最大值
    public static final long DATA_CENTER_MAX = 1 << DATA_CENTER_BIT - 1;
    public static final long MACHINE_MAX = 1 << MACHINE_BIT - 1;
    public static final long SEQUENCE_MAX = 1 << SEQUENCE_BIT - 1;

    // 时间戳（42） 机房号（5） 机器号（5） 序列号（12）
    public static final long TIMESTAMP_LEFT = DATA_CENTER_BIT + MACHINE_BIT + SEQUENCE_BIT;
    public static final long DATA_CENTER_LEFT = MACHINE_BIT + SEQUENCE_BIT;
    public static final long MACHINE_LEFT = SEQUENCE_BIT;

    private long dataCenterId;
    private long machineId;
    private LongAdder sequenceId = new LongAdder();
    // 时钟回拨的问题，我们需要去处理
    private long lastTimestamp = -1L;

    public IdGenerator(long dataCenterId, long machineId) {
        // 判断参数是否合法
        if(dataCenterId > DATA_CENTER_MAX || machineId > MACHINE_MAX) {
            throw new IllegalArgumentException("传入的数据中心编号或机器号不合法.");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public long getId() {
        // 1.处理时间戳
        long currentTime = System.currentTimeMillis();

        long timeStamp = currentTime - START_STAMP;

        // 判断时钟回拨
        if(timeStamp < lastTimestamp) {
            throw new RuntimeException("您的服务器进行了时钟回拨");
        }

        // sequenceId需要处理，如果是统一时间节点，必须自增
        if(timeStamp == lastTimestamp) {
            // todo ++自增线程不安全
            sequenceId.increment();
            if(sequenceId.sum() >= SEQUENCE_MAX) {
                timeStamp = getNextTimeStamp();
                sequenceId.reset();
            }
        } else {
            sequenceId.reset();
        }
        // 执行结束将时间戳赋值给lastTimeStamp
        lastTimestamp = timeStamp;
        long sequence = sequenceId.sum();
        return timeStamp << TIMESTAMP_LEFT | dataCenterId << DATA_CENTER_LEFT | machineId << MACHINE_LEFT | sequence;

    }

    private long getNextTimeStamp() {
        long current = System.currentTimeMillis() - START_STAMP;
        // 循环到下一个时间戳
        while(current == lastTimestamp) {
            current = System.currentTimeMillis() - START_STAMP;
        }

        return current;
    }
}

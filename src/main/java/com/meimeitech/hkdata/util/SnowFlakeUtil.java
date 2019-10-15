package com.meimeitech.hkdata.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 原作者 zzxadi https://github.com/zzxadi/Snowflake-IdWorker
 * @author root
 */
@Slf4j
public class SnowFlakeUtil {
    public static void init(){}

    private final long id;
    /**
     * 时间起始标记点，作为基准，一般取系统的最近时间//2019/01/01 00:00:00
     */
    private final long epoch = 1546272000000L;
    /**
     * 机器标识位数
     */
    private final long workerIdBits = 10L;
    /**
     * 机器ID最大值: 1023
     */
    private final long maxWorkerId = -1L ^ -1L << this.workerIdBits;
    /**
     * 0，并发控制
     */
    private long sequence = 0L;
    /**
     * 毫秒内自增位
     */
    private final long sequenceBits = 12L;

    /**
     * 12
     */
    private final long workerIdShift = this.sequenceBits;
    /**
     * 22
     */
    private final long timestampLeftShift = this.sequenceBits + this.workerIdBits;
    /**
     * 4095,111111111111,12位
     */
    private final long sequenceMask = -1L ^ -1L << this.sequenceBits;
    private long lastTimestamp = -1L;

    private SnowFlakeUtil(long id) {
        if (id > this.maxWorkerId || id < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", this.maxWorkerId));
        }
        this.id = id;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (this.lastTimestamp == timestamp) {
            //如果上一个timestamp与新产生的相等，则sequence加一(0-4095循环); 对新的timestamp，sequence从0开始
            this.sequence = this.sequence + 1 & this.sequenceMask;
            if (this.sequence == 0) {
                // 重新生成timestamp
                timestamp = this.tilNextMillis(this.lastTimestamp);
            }
        } else {
            this.sequence = 0;
        }

        if (timestamp < this.lastTimestamp) {
            log.error(String.format("clock moved backwards.Refusing to generate id for %d milliseconds", (this.lastTimestamp - timestamp)));
            return -1;
        }

        this.lastTimestamp = timestamp;

        return (timestamp - this.epoch) << this.timestampLeftShift | this.id << this.workerIdShift | this.sequence;
    }
    public static int MACHINE_NO ;
    static {
        String property = System.getProperty("machine.no");
        try {
            int i = Integer.parseInt(property.trim());
            if (0 <= i && i <= 1023 ){
                log.info("当前机器号:-Dmachine.no="+i);
            }else {
                throw new RuntimeException();
            }
            MACHINE_NO = i;
        }catch (Exception e){
            log.error("当前机器号:-Dmachine.no,配置不在区间[0,1023]范围内或者没有配置.注:该配置影响主键id生成,集群中要唯一");
            System.exit(1);
        }
    }

    private static SnowFlakeUtil flowIdWorker = new SnowFlakeUtil(MACHINE_NO);
    public static SnowFlakeUtil getFlowIdInstance() {
        return flowIdWorker;
    }

    /**
     * 等待下一个毫秒的到来, 保证返回的毫秒数在参数lastTimestamp之后
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获得系统当前毫秒数
     */
    private static long timeGen() {
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SnowFlakeUtil snowFlakeUtil1 = new SnowFlakeUtil(5);
        long l = snowFlakeUtil1.nextId();
        System.out.println(l);
        SnowFlakeUtil snowFlakeUtil2 = new SnowFlakeUtil(7);
        long l1 = snowFlakeUtil2.nextId();
        System.out.println(l1);
    }
}

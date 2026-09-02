package com.bbpms.common.util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1700000000000L;
    private static final long SEQ_BITS = 12L, WORKER_BITS = 5L, DC_BITS = 5L;
    private static final long MAX_SEQ = -1L ^ (-1L << SEQ_BITS);
    private static final long MAX_WORKER = -1L ^ (-1L << WORKER_BITS);
    private static final long MAX_DC = -1L ^ (-1L << DC_BITS);
    private final long workerId, datacenterId;
    private long sequence = 0L, lastTs = -1L;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER || workerId < 0) throw new IllegalArgumentException("workerId");
        if (datacenterId > MAX_DC || datacenterId < 0) throw new IllegalArgumentException("datacenterId");
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) throw new RuntimeException("Clock moved backwards");
        if (ts == lastTs) { sequence = (sequence + 1) & MAX_SEQ; if (sequence == 0L) ts = tilNext(lastTs); }
        else sequence = 0L;
        lastTs = ts;
        return (ts - EPOCH) << (SEQ_BITS + WORKER_BITS + DC_BITS) | datacenterId << (SEQ_BITS + WORKER_BITS) | workerId << SEQ_BITS | sequence;
    }

    private long tilNext(long last) { long t; do { t = System.currentTimeMillis(); } while (t <= last); return t; }
}

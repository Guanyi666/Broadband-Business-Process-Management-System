package com.bbpms.common.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Redisson-backed implementation of DistributedLock. */
@Component
@RequiredArgsConstructor
public class RedissonDistributedLock implements DistributedLock {

    private final RedissonClient redisson;

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        if (key == null) return false;
        RLock lock = redisson.getLock(key);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean unlock(String key) {
        if (key == null) return false;
        RLock lock = redisson.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            return true;
        }
        return false;
    }
}

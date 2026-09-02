package com.bbpms.common.lock;

import java.util.concurrent.TimeUnit;

/** Abstraction for cluster-wide locking. */
public interface DistributedLock {

    /** Try to acquire. Returns null on success, otherwise a release token; null means acquired. */
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /** Release a previously acquired lock. */
    boolean unlock(String key);
}

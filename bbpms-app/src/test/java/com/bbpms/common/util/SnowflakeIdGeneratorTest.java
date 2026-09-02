package com.bbpms.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SnowflakeIdGenerator}.
 *
 * <p>No Spring context — pure Java, safe to run in CI without MySQL/Redis.
 */
class SnowflakeIdGeneratorTest {

    @Test
    void shouldGenerateMonotonicallyIncreasingIdsWithinSameMs() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1L, 1L);

        long first = gen.nextId();
        long second = gen.nextId();
        long third = gen.nextId();

        // Same millisecond, same worker/datacenter — sequence guarantees ordering
        assertTrue(first < second, "second id should be > first");
        assertTrue(second < third, "third id should be > second");
    }

    @Test
    void shouldProduceUniqueIdsUnderConcurrentLoad() throws Exception {
        // 4 threads × 5000 ids = 20000; generator must give back 20000 distinct longs
        int threads = 4;
        int perThread = 5000;
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(2L, 1L);
        Set<Long> seen = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) { return; }
                for (int i = 0; i < perThread; i++) {
                    if (!seen.add(gen.nextId())) {
                        throw new AssertionError("duplicate id detected under concurrency");
                    }
                }
            });
        }
        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "generator too slow under load");
        assertEquals(threads * perThread, seen.size());
    }

    @Test
    void shouldEmbedWorkerAndDatacenterInId() {
        long workerId = 7L;
        long dcId = 3L;
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(workerId, dcId);
        long id = gen.nextId();

        // Sequence mask = 12 bits, worker shift = 12, dc shift = 17
        long seqMask = -1L ^ (-1L << 12);
        long workerMask = -1L ^ (-1L << 5);

        long worker = (id >> 12) & workerMask;
        long dc = (id >> 17) & workerMask; // 5-bit datacenter at the same mask size

        assertEquals(workerId, worker, "worker id should round-trip");
        assertEquals(dcId, dc, "datacenter id should round-trip");
        // Sequence occupies the low 12 bits and may be 0..4095
        long seq = id & seqMask;
        assertTrue(seq >= 0 && seq <= seqMask);
    }

    @Test
    void shouldRejectInvalidWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(32L, 1L)); // 5 bits = 0..31
    }

    @Test
    void shouldRejectInvalidDatacenterId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1L, -1L));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1L, 32L));
    }

    @Test
    void shouldProduceDifferentIdsForDifferentWorkers() {
        SnowflakeIdGenerator a = new SnowflakeIdGenerator(0L, 0L);
        SnowflakeIdGenerator b = new SnowflakeIdGenerator(1L, 0L);

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(a.nextId());
            ids.add(b.nextId());
        }
        // 100 + 100 = 200, all unique
        assertEquals(200, ids.size());

        // And ids from worker 0 and worker 1 should not be equal at any point
        // (worker is part of the bit layout, so first id from each must differ)
        SnowflakeIdGenerator x = new SnowflakeIdGenerator(0L, 0L);
        SnowflakeIdGenerator y = new SnowflakeIdGenerator(1L, 0L);
        assertNotEquals(x.nextId(), y.nextId());
    }
}

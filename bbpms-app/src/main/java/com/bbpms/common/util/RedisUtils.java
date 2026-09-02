package com.bbpms.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Thin null-safe wrapper over StringRedisTemplate.
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final StringRedisTemplate redis;

    /* -------------------- string -------------------- */
    public void set(String key, String value) {
        if (key == null) return;
        redis.opsForValue().set(key, value);
    }

    public void set(String key, String value, long ttl, TimeUnit unit) {
        if (key == null) return;
        redis.opsForValue().set(key, value, ttl, unit);
    }

    public String get(String key) {
        if (key == null) return null;
        return redis.opsForValue().get(key);
    }

    public Boolean del(String key) {
        if (key == null) return Boolean.FALSE;
        return redis.delete(key);
    }

    public Long del(Set<String> keys) {
        if (keys == null || keys.isEmpty()) return 0L;
        Long n = redis.delete(keys);
        return n == null ? 0L : n;
    }

    public Boolean expire(String key, long ttl, TimeUnit unit) {
        if (key == null) return Boolean.FALSE;
        return redis.expire(key, ttl, unit);
    }

    public Long incr(String key) {
        if (key == null) return null;
        return redis.opsForValue().increment(key);
    }

    public Long incr(String key, long delta) {
        if (key == null) return null;
        return redis.opsForValue().increment(key, delta);
    }

    public Long decr(String key) {
        if (key == null) return null;
        return redis.opsForValue().decrement(key);
    }

    public Long decr(String key, long delta) {
        if (key == null) return null;
        return redis.opsForValue().decrement(key, delta);
    }

    /* -------------------- hash -------------------- */
    public void hSet(String key, String field, String value) {
        if (key == null || field == null) return;
        redis.opsForHash().put(key, field, value);
    }

    public String hGet(String key, String field) {
        if (key == null || field == null) return null;
        Object v = redis.opsForHash().get(key, field);
        return v == null ? null : v.toString();
    }

    public Map<Object, Object> hGetAll(String key) {
        if (key == null) return Collections.emptyMap();
        return redis.opsForHash().entries(key);
    }

    /* -------------------- set -------------------- */
    public Long sAdd(String key, String... values) {
        if (key == null || values == null || values.length == 0) return 0L;
        Long n = redis.opsForSet().add(key, values);
        return n == null ? 0L : n;
    }

    public Set<String> sMembers(String key) {
        if (key == null) return Collections.emptySet();
        Set<String> m = redis.opsForSet().members(key);
        return m == null ? Collections.emptySet() : m;
    }

    public Boolean sIsMember(String key, String value) {
        if (key == null || value == null) return Boolean.FALSE;
        Boolean b = redis.opsForSet().isMember(key, value);
        return Boolean.TRUE.equals(b);
    }

    /* -------------------- zset -------------------- */
    public Boolean zAdd(String key, String value, double score) {
        if (key == null || value == null) return Boolean.FALSE;
        return redis.opsForZSet().add(key, value, score);
    }

    public Set<String> zRange(String key, long start, long end) {
        if (key == null) return Collections.emptySet();
        Set<String> r = redis.opsForZSet().range(key, start, end);
        return r == null ? Collections.emptySet() : r;
    }

    public Set<String> zRangeByScore(String key, double min, double max) {
        if (key == null) return Collections.emptySet();
        Set<String> r = redis.opsForZSet().rangeByScore(key, min, max);
        return r == null ? Collections.emptySet() : r;
    }

    public Double zIncrBy(String key, String value, double delta) {
        if (key == null || value == null) return null;
        return redis.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * Remove a single member from a sorted set.
     * @return number of members removed (0 if key or member missing)
     */
    public Long zRem(String key, Object value) {
        if (key == null || value == null) return 0L;
        Long n = redis.opsForZSet().remove(key, value);
        return n == null ? 0L : n;
    }

    /**
     * Remove all members with score in {@code [min, max]} from a sorted set.
     * Used to garbage-collect stale heartbeat entries whose score is older
     * than the freshness window.
     *
     * @return number of members removed
     */
    public Long zRemRangeByScore(String key, double min, double max) {
        if (key == null) return 0L;
        Long n = redis.opsForZSet().removeRangeByScore(key, min, max);
        return n == null ? 0L : n;
    }

    /**
     * Return the current score of a member, or {@code null} if missing.
     * Used to detect heartbeat freshness without fetching the whole set.
     */
    public Double zScore(String key, Object value) {
        if (key == null || value == null) return null;
        return redis.opsForZSet().score(key, value);
    }

    /* -------------------- distributed lock (setnx + lua release) -------------------- */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    public boolean tryLock(String key, String requestId, long ttl, TimeUnit unit) {
        if (key == null || requestId == null) return false;
        Boolean ok = redis.opsForValue().setIfAbsent(key, requestId, ttl, unit);
        return Boolean.TRUE.equals(ok);
    }

    public boolean releaseLock(String key, String requestId) {
        if (key == null || requestId == null) return false;
        Long res = redis.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection ->
                connection.scriptingCommands().eval(
                        UNLOCK_LUA.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        key.getBytes(),
                        requestId.getBytes()));
        return res != null && res > 0;
    }
}

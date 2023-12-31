package com.fit.cache.cache;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * @author songhao
 */
public class CaffeineBuilder {

    /**
     * 构建所有来的要缓存的key getCache
     */
    public static Cache<String, Object> cache(int minSize, int maxSize, int expireSeconds) {
        return Caffeine.newBuilder().initialCapacity(minSize).maximumSize(maxSize)
            .expireAfterWrite(expireSeconds, TimeUnit.SECONDS).build();
    }

}

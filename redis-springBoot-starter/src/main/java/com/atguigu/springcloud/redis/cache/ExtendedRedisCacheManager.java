
package com.atguigu.springcloud.redis.cache;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.Objects;

@Log4j2
@Data
public class ExtendedRedisCacheManager extends RedisCacheManager {
    /**
     * 分隔符
     */
    private static final char SEPARATOR = '#';

    public ExtendedRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }
    @Override
    synchronized
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        int index = name.indexOf(SEPARATOR);
        Duration defaultTtl = cacheConfig.getTtl();
        if (index > 0) {
            Duration ttl = getExpiration(name, index, defaultTtl);
            cacheConfig= cacheConfig.entryTtl(ttl);
        }
        RedisCache redisCache = super.createRedisCache(name, cacheConfig);
        // name 中获取过期时间
        return redisCache;
    }

    protected Duration getExpiration(final String name, final int separatorIndex, Duration defalutExp) {
        Long expiration = null;
        String expirationAsString = name.substring(separatorIndex + 1);
        try {
            expiration = NumberUtils.toLong(expirationAsString);
        } catch (Exception e) {
            log.error("缓存时间转换错误:{},异常:{}", name, e.getMessage());
        }
        return Objects.nonNull(expiration) ? Duration.ofMinutes(expiration.longValue()) : defalutExp;
    }
}

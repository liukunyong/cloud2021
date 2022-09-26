/**
 * Copyright (C), 2019, 兆尹
 * FileName: RedisAutoConfig
 * Author:   Lios
 * Date:     2019/9/25 0025 1:29
 * Description:
 */
package com.atguigu.springcloud.redis;

import com.atguigu.springcloud.redis.cache.ExtendedRedisCacheManager;
import com.atguigu.springcloud.redis.serializer.RedisObjectSerializer;
import com.atguigu.springcloud.redis.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@AutoConfigureBefore(RedisTemplate.class)
@EnableCaching
public class RedisAutoConfig {

    @Autowired(required = false)
    private LettuceConnectionFactory lettuceConnectionFactory;
    @Autowired
    private RedisProperties redisProperties;

    public final static String REDIS_TEMPLATE_14 = "redisTemplate14";
    public final static String REDIS_TEMPLATE_15 = "redisTemplate15";

    /**
     * 适配redis cluster单节点
     *
     * @return
     */
    @Primary
    @Bean("redisTemplate")
    @ConditionalOnProperty(name = "spring.redis.cluster.nodes", matchIfMissing = false)
    public RedisTemplate<String, Object> getRedisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        // 设置redisTemplate参数
        initRedisTemplate(redisTemplate);
        return redisTemplate;
    }

//    @Bean(REDIS_TEMPLATE_14)
//    @ConditionalOnProperty(name = "spring.redis.cluster.nodes", matchIfMissing = false)
//    public RedisTemplate<String, Object> getRedisTemplate14() {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setConnectionFactory(LettuceConnectionFactoryUtil.createLettuceConnectionFactory(
//                redisProperties, RedisDbIndexConstant.KEYBOARD_SEARCH));
//        // 设置redisTemplate参数
//        initRedisTemplate(redisTemplate);
//        return redisTemplate;
//    }
//
//    @Bean(REDIS_TEMPLATE_15)
//    @ConditionalOnProperty(name = "spring.redis.cluster.nodes", matchIfMissing = false)
//    public RedisTemplate<String, Object> getRedisTemplate15() {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setConnectionFactory(LettuceConnectionFactoryUtil.createLettuceConnectionFactory(
//                redisProperties, RedisDbIndexConstant.KEYBOARD_CONFIG));
//        // 设置redisTemplate参数
//        initRedisTemplate(redisTemplate);
//        return redisTemplate;
//    }

    /**
     * 适配redis单节点
     *
     * @return
     */
    @Primary
    @Bean("redisTemplate")
    @ConditionalOnProperty(name = "spring.redis.host", matchIfMissing = true)
    public RedisTemplate<String, Object> getSingleRedisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        // 设置redisTemplate参数
        initRedisTemplate(redisTemplate);
        return redisTemplate;
    }

//    @Bean(REDIS_TEMPLATE_14)
//    @ConditionalOnProperty(name = "spring.redis.host", matchIfMissing = true)
//    public RedisTemplate<String, Object> getSingleRedisTemplate14() {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setConnectionFactory(LettuceConnectionFactoryUtil.createLettuceConnectionFactory(
//                redisProperties, RedisDbIndexConstant.KEYBOARD_SEARCH));
//        // 设置redisTemplate参数
//        initRedisTemplate(redisTemplate);
//        return redisTemplate;
//    }
//
//    @Bean(REDIS_TEMPLATE_15)
//    @ConditionalOnProperty(name = "spring.redis.host", matchIfMissing = true)
//    public RedisTemplate<String, Object> getSingleRedisTemplate15() {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setConnectionFactory(LettuceConnectionFactoryUtil.createLettuceConnectionFactory(
//                redisProperties, RedisDbIndexConstant.KEYBOARD_CONFIG));
//        // 设置redisTemplate参数
//        initRedisTemplate(redisTemplate);
//        return redisTemplate;
//    }

    @Bean
    public HashOperations<String, String, String> hashOperations(StringRedisTemplate stringRedisTemplate) {
        return stringRedisTemplate.opsForHash();
    }

    /**
     * redis工具类
     */
    @Bean("redisUtil")
    public RedisUtil redisUtil(RedisTemplate<String, Object> redisTemplate,
                               StringRedisTemplate stringRedisTemplate,
                               HashOperations<String, String, String> hashOperations) {
        RedisUtil redisUtil = new RedisUtil(redisTemplate, stringRedisTemplate, hashOperations);
        redisUtil.setRedisObjectSerializer(valueSerializer());
        return redisUtil;
    }

    /***
     * 功能描述: <br>
     * redis缓存数据
     * @Param redisConnectionFactory: redis连接
     * @return: org.springframework.data.redis.cache.RedisCacheManager
     * @since: 1.0.0
     * @Author: Lios
     * @Date: 2019/9/29 0029 12:43
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        RedisCacheWriter writer = RedisCacheWriter.lockingRedisCacheWriter(redisConnectionFactory);

        // 默认缓存不过期
        RedisCacheConfiguration redisCacheConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                        // .entryTtl(Duration.ofHours(240L))
                        .disableCachingNullValues()
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer((valueSerializer())));
        return new ExtendedRedisCacheManager(writer, redisCacheConfiguration);
    }

    /***
     * 功能描述: <br>
     * redis value值得序列化方式 jackson
     * @return: org.springframework.data.redis.serializer.RedisSerializer<java.lang.Object>
     * @since: 1.0.0
     * @Author: Lios
     * @Date: 2019/11/19 0019 10:45
     */
    private RedisSerializer<Object> valueSerializer() {
        return new RedisObjectSerializer();
    }

    /***
     * 功能描述: <br>
     * key序列化方式
     * @return: org.springframework.data.redis.serializer.RedisSerializer<java.lang.String>
     * @since: 1.0.0
     * @Author: Lios
     * @Date: 2019/11/19 0019 11:18
     */
    private RedisSerializer<String> keySerializer() {
        return new StringRedisSerializer();
    }

    /**
     * 设置redisTemplate参数
     *
     * @param redisTemplate
     */
    private void initRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        // key的序列化类型
        redisTemplate.setKeySerializer(keySerializer());
        redisTemplate.setHashKeySerializer(keySerializer());

        // value的序列化类型
        redisTemplate.setValueSerializer(valueSerializer());
        redisTemplate.setHashValueSerializer(valueSerializer());
        redisTemplate.afterPropertiesSet();
    }
}

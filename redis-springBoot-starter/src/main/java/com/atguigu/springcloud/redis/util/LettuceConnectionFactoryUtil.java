package com.atguigu.springcloud.redis.util;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

public class LettuceConnectionFactoryUtil {

    public static LettuceConnectionFactory createLettuceConnectionFactory(RedisProperties redisProperties, int dbIndex) {
        String hostName = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();
        Long timeOut = redisProperties.getTimeout().toMillis();
        int maxIdle = redisProperties.getLettuce().getPool().getMaxIdle();
        int minIdle = redisProperties.getLettuce().getPool().getMinIdle();
        int maxActive = redisProperties.getLettuce().getPool().getMaxActive();
        Long maxWait = redisProperties.getLettuce().getPool().getMaxWait().toMillis();
        Long shutdownTimeOut = redisProperties.getLettuce().getShutdownTimeout().toMillis();

        //redis配置
        RedisConfiguration redisConfiguration = new RedisStandaloneConfiguration(hostName, port);
        ((RedisStandaloneConfiguration) redisConfiguration).setDatabase(dbIndex);
        ((RedisStandaloneConfiguration) redisConfiguration).setPassword(password);

        //连接池配置
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMaxWaitMillis(maxWait);

        //redis客户端配置
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder
                = LettucePoolingClientConfiguration.builder().commandTimeout(Duration.ofMillis(timeOut));
        builder.shutdownTimeout(Duration.ofMillis(shutdownTimeOut));
        builder.poolConfig(genericObjectPoolConfig);
        LettuceClientConfiguration lettuceClientConfiguration = builder.build();

        //根据配置和客户端配置创建连接
        LettuceConnectionFactory lettuceConnectionFactory = new
                LettuceConnectionFactory(redisConfiguration, lettuceClientConfiguration);
        lettuceConnectionFactory.afterPropertiesSet();

        return lettuceConnectionFactory;
    }
}

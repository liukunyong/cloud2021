
package com.atguigu.springcloud.redis.util;

 
public interface RedisSubscribeCallback {
    void callback(String msg);
}

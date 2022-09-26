
package com.atguigu.springcloud.redis.util;

import com.atguigu.springcloud.redis.serializer.RedisObjectSerializer;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.query.SortQueryBuilder;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
public class RedisUtil {

    /**
     * 批量操作每次数量
     */
    public final static int BATCH_SIZE = 10000;
    /**
     * key排除关键字
     */
    public final static String KEY_PATTERN_EXCLUEDE = "@";

    private StringRedisTemplate stringRedisTemplate;

    private HashOperations<String, String, String> hashOperations;

    /**
     * json序列化方式
     */
    private static RedisSerializer<Object> redisObjectSerializer = new RedisObjectSerializer();

    public void setRedisObjectSerializer(RedisSerializer redisObjectSerializer) {
        redisObjectSerializer = redisObjectSerializer;
    }

    public RedisSerializer<Object> getRedisObjectSerializer() {
        return redisObjectSerializer;
    }

    /**
     * 默认RedisObjectSerializer序列化
     */
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * redis工具类
     *
     * @param redisTemplate
     * @param stringRedisTemplate
     * @param hashOperations
     */
    public RedisUtil(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate,
                     HashOperations<String, String, String> hashOperations) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.hashOperations = hashOperations;
    }


    /**
     * 获取消息
     *
     * @param topicGroup
     * @param channel
     * @return
     */
    public String getMsg(String topicGroup, String channel) {
        return hashOperations.get(topicGroup, channel);
    }

    /**
     * 删除消息
     *
     * @param topicGroup
     * @param channel
     * @return
     */
    public boolean removeMsg(String topicGroup, String channel) {
        publishMsg(topicGroup, channel, StringUtils.EMPTY);

        return hashOperations.delete(topicGroup, channel) == 1;
    }

    /**
     * 发送消息，存redis hash
     *
     * @param topicGroup
     * @param channel
     * @param msg
     * @return
     */
    public boolean publishMsg(String topicGroup, String channel, String msg) {
        hashOperations.put(topicGroup, channel, msg);
        //向通道发送消息的方法
        stringRedisTemplate.convertAndSend(topicGroup + "-" + channel, msg);

        return true;
    }

    /**
     * 订阅回调
     *
     * @param msg
     * @param redisSubscribeCallback
     */
    public void subscribeConfig(String msg, RedisSubscribeCallback redisSubscribeCallback) {
        redisSubscribeCallback.callback(msg);
    }

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key, long time) {
        return redisTemplate.execute(new RedisCallback<Boolean>() {

            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                long rawTimeout = TimeoutUtils.toMillis(time, TimeUnit.SECONDS);
                try {
                    return connection.pExpire(key.getBytes(), rawTimeout);
                } catch (Exception e) {
                    // Driver may not support pExpire or we may be running on
                    // Redis 2.4
                    return connection.expire(key.getBytes(), TimeoutUtils.toSeconds(rawTimeout, TimeUnit.SECONDS));
                }
            }
        });
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {

        return redisTemplate.execute(new RedisCallback<Long>() {

            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {

                try {
                    return connection.pTtl(key.getBytes(), TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Driver may not support pTtl or we may be running on Redis
                    // 2.4
                    return connection.ttl(key.getBytes(), TimeUnit.SECONDS);
                }
            }
        });
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection -> connection.exists(key.getBytes()));
    }

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return get(redisTemplate, key);
    }

    public Object get(RedisTemplate redisTemplate, String key) {
        Object value = redisTemplate.execute(new RedisCallback<Object>() {

            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {

                // redis info
                byte[] temp = null;
                temp = connection.get(key.getBytes());
                connection.close();

                return redisObjectSerializer.deserialize(temp);
            }
        });

        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        return set(redisTemplate, key, value);
    }

    public boolean set(RedisTemplate redisTemplate, String key, Object value) {
        try {

            redisTemplate.execute((RedisCallback<Long>) connection -> {
                // redis info
                byte[] values = redisObjectSerializer.serialize(value);
                connection.set(key.getBytes(), values);
                connection.close();

                return 1L;
            });
            return true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("set-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(分钟) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {

                redisTemplate.execute((RedisCallback<Long>) connection -> {
                    // redis info
                    byte[] values = redisObjectSerializer.serialize(value);
                    connection.set(key.getBytes(), values);
                    connection.expire(key.getBytes(), 60 * time);
                    connection.close();
                    return 1L;
                });

            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("set-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.execute((RedisCallback<Long>) connection -> {

            return connection.incrBy(key.getBytes(), delta);
        });
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }

        return redisTemplate.execute((RedisCallback<Long>) connection -> {

            return connection.incrBy(key.getBytes(), -delta);
        });

    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }

    /**
     * 写入缓存
     *
     * @param key
     * @param offset 位 8Bit=1Byte
     * @return
     */
    public boolean setBit(String key, long offset, boolean isShow) {
        boolean result = false;
        try {
            ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            operations.setBit(key, offset, isShow);
            result = true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("setBit-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
        return result;
    }

    /**
     * 写入缓存
     *
     * @param key
     * @param offset
     * @return
     */
    public boolean getBit(String key, long offset) {
        boolean result = false;
        try {
            ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            result = operations.getBit(key, offset);
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("getBit-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
        return result;
    }

    /***
     * 功能描述: <br>
     * 根据 pattern模糊匹配key
     * @Param pattern: 模糊匹配格式
     * @return: java.util.Set<java.lang.String>
     * @since: 1.0.0
     * @Author: Lios
     * @Date: 2019/11/27 0027 17:45
     */
    public Set<String> keys(String pattern) {
        return keys(redisTemplate, pattern);
    }

    public Set<String> keys(RedisTemplate redisTemplate, String pattern) {
        return redisTemplate.keys(pattern);
    }

    /***
     * 功能描述: <br>
     * 根据 pattern模糊匹配key 删除格式
     * @Param pattern: 模糊匹配格式
     * @return: void
     * @since: 1.0.0
     * @Author: Lios
     * @Date: 2019/11/27 0027 17:49
     */
    public void delByPattern(String pattern) {
        Set<String> keys = keys(pattern);

        if (keys == null) {
            return;
        }
        del(keys.toArray(new String[]{}));
    }

    //===============================list=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束  0 到 -1代表所有值
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lGet-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lGetListSize-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引  index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lGetIndex-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lSet-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lSet-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lSet-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lSet-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lUpdateIndex-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            log.error("lRemove-" + stackTraceElement.getMethodName() + "--" + stackTraceElement.getLineNumber());
            return 0;
        }
    }


    // ================================Map=================================

    /**
     * 哈希 添加
     *
     * @param key
     * @param hashKey
     * @param value
     */
    public void hmSet(String key, Object hashKey, Object value) {
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        hash.put(key, hashKey, value);
    }

    /**
     * 哈希获取数据
     *
     * @param key
     * @param hashKey
     * @return
     */
    public Object hmGet(String key, Object hashKey) {
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        return hash.get(key, hashKey);
    }


    // ================================set=================================

    /**
     * 集合添加
     *
     * @param key
     * @param value
     */
    public void add(String key, Object value) {
        SetOperations<String, Object> set = redisTemplate.opsForSet();
        set.add(key, value);
    }

    /**
     * 集合获取
     *
     * @param key
     * @return
     */
    public Set<Object> setMembers(String key) {
        SetOperations<String, Object> set = redisTemplate.opsForSet();
        return set.members(key);
    }

    /**
     * 有序集合添加
     *
     * @param key
     * @param value
     * @param scoure
     */
    public void zAdd(String key, Object value, double scoure) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.add(key, value, scoure);
    }

    /** 
     * @Description:  有序集合zset批量增加
     * @Param:   
     * @return:   
     * @Author: 张宏胜
     * @Date: 20/3/24 22:04
     */ 
    public Long zSetAddList(String key, Set<ZSetOperations.TypedTuple<Object>> typedTupleSet ) {
//        ZSetOperations.TypedTuple typedTuple = new DefaultTypedTuple("key", null);
        return this.redisTemplate.opsForZSet().add(key, typedTupleSet);
    }

    public Cursor<ZSetOperations.TypedTuple<Object>> zSetScan(String key, String pattern) {
        return this.redisTemplate.opsForZSet().scan(key, ScanOptions.scanOptions().match(pattern).count(10000).build());
    }

    /**
     * 有序集合获取
     *
     * @param key
     * @param scoure
     * @param scoure1
     * @return
     */
    public Set<Object> rangeByScore(String key, double scoure, double scoure1) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        redisTemplate.opsForValue();
        return zset.rangeByScore(key, scoure, scoure1);
    }


    /**
     * 有序集合获取排名
     *
     * @param key   集合名称
     * @param value 值
     */
    public Long zRank(String key, Object value) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.rank(key, value);
    }

    /**
     * 有序集合获取排名
     *
     * @param key
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRankWithScore(String key, long start, long end) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> ret = zset.rangeWithScores(key, start, end);
        return ret;
    }

    /**
     * 有序集合添加
     *
     * @param key
     * @param value
     */
    public Double zSetScore(String key, Object value) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.score(key, value);
    }

    /**
     * 有序集合添加分数
     *
     * @param key
     * @param value
     * @param scoure
     */
    public void incrementScore(String key, Object value, double scoure) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.incrementScore(key, value, scoure);
    }

    /**
     * 有序集合获取排名
     *
     * @param key
     */
    public Set<ZSetOperations.TypedTuple<Object>> reverseZrankWithScore(String key, long start, long end) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> ret = zset.reverseRangeByScoreWithScores(key, start, end);
        return ret;
    }

    /**
     * 有序集合获取排名
     *
     * @param key
     */
    public Set<ZSetOperations.TypedTuple<Object>> reverseZrankWithRank(String key, long start, long end) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> ret = zset.reverseRangeWithScores(key, start, end);
        return ret;
    }


    // =========================== 排序 ======================

    /**
     * @Description: 分页查询
     *
     * @Param: key   redis中的键
     * @Param: pattern  匹配表达式
     * @Param: offset 分页起始值
     * @Param: count 每页数据量
     * @return:
     * @Author: 张宏胜
     * @Date: 20/3/26 15:42
     */
    public List<Object> sortAndPage(String key, String pattern, Long offset, Long count) {
        // redis中的键
        SortQueryBuilder<String> builder = SortQueryBuilder.sort(key);
        // 匹配模式
        builder.by(pattern);
        builder.get("#");
        // 和mysql的limit一样，排序后，从第offset个元素开始，取出count个元素
        builder.limit(offset, count);
        // 升降序
        builder.order(SortParameters.Order.ASC);
        // 是否按照字符串顺序
//        builder.alphabetical(true);

        return redisTemplate.sort(builder.build());
    }


    // =========================== 批量  =====================

    /**
     * 批量查询
     *
     * @param pattern
     * @return
     */
    public List<Object> multiGet(String pattern) {
        return multiGet(redisTemplate, pattern);
    }
    public List<Object> multiGetByKeys(Set<String> keys) {
        return multiGetByKeys(redisTemplate, keys);
    }

    /**
     * 批量插入
     *
     * @param pattern
     * @return
     */
    public void multiSet(Map<String, Object> data) {
        // 批量设置数据
        redisTemplate.opsForValue().multiSet(data);
    }

    /**
     * 批量删除
     *
     * @param pattern
     * @return
     */
    public Long multiDel(Collection<String> keys) {
        // 批量删除数据
        return redisTemplate.delete(keys);
    }

    public List<Object> multiGet(RedisTemplate redisTemplate, String pattern) {
        Set<String> keys = keys(redisTemplate, pattern);
        return multiGetByKeys(redisTemplate, keys);
    }

    public List<Object> multiGetByKeys(RedisTemplate redisTemplate, Set<String> keys) {
        int size = keys.size();
        if (size > BATCH_SIZE) {
            Object[] keysArray = keys.toArray();

            List<Object> myObjectListRedis = new ArrayList<>(size);
            int start = 0;
            int end = BATCH_SIZE;
            Set<String> tempKes = new HashSet<>(BATCH_SIZE);
            while (start < size) {
                // 批量获取数据
                for (int i = start; i < end; i++) {
                    tempKes.add((String) keysArray[i]);
                }
                myObjectListRedis.addAll(redisTemplate.opsForValue().multiGet(tempKes));

                tempKes.clear();
                start += BATCH_SIZE;
                end += BATCH_SIZE;
                if (end > size) {
                    end = size;
                }
            }
            return myObjectListRedis;
        }

        return redisTemplate.opsForValue().multiGet(keys);
    }

    public void multiSet(RedisTemplate redisTemplate, Map<String, Object> data) {
        // 批量设置数据
        redisTemplate.opsForValue().multiSet(data);
    }

    public Long multiDel(RedisTemplate redisTemplate, Collection<String> keys) {
        // 批量删除数据
        return redisTemplate.delete(keys);
    }

    /**
     * 删除hash数据
     * @param key
     * @param hashkey
     */
    public  void delHash(String key,String hashkey){
        Boolean flag = redisTemplate.opsForHash().hasKey(key, hashkey);
        if(true==flag){
            redisTemplate.opsForHash().delete(key, hashkey);
        }
    }

}

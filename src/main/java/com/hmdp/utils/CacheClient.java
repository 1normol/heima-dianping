package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.SystemConstants.*;

@Component
public class CacheClient {
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 设置缓存空值解决缓存穿透。
     */
    public <R,ID> R getInfoThroughCache(String keyPrefix, ID id, Function<ID,R> dbFallBack,Long expireTime,TimeUnit timeUnit){
        Object o = redisTemplate.opsForValue().get(keyPrefix+id);
        if (ObjectUtils.isNotEmpty(o)){
            return (R) o;
        }
        if (o != null){
            //该对象既不为null,又无字段属性，说明走了缓存空值，直接返回错误。
            return null;
        }
        R r = dbFallBack.apply(id);
            if (r == null){
                redisTemplate.opsForValue().set(keyPrefix+id,"",expireTime,timeUnit);
                return null;
            }
            redisTemplate.opsForValue().set(keyPrefix+id,r,expireTime,timeUnit);
            return r;
        }

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    /** 逻辑过期时间解决缓存雪崩
     * return R
     */
    public <R,ID> R getInfoLogicExpired(String keyPrefix,ID id,Function<ID,R> dbFallBack,Long expireTime,TimeUnit timeUnit)  {
        String key = keyPrefix + id;
        Object o = redisTemplate.opsForValue().get(key);
        if (ObjectUtils.isEmpty(o)){
            return null;
        }
        RedisData redisData = (RedisData) o;
        //如果还未过期,直接返回
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())){
            return (R) redisData.getData();
        }
        //过期，使用互斥锁，另起一个线程，异步重置缓存
        String lock_key = "lock:"+id;
        boolean isLock = getLock(lock_key);
        if (isLock){
            threadPool.submit(()->{
                try {
                    System.out.println(Thread.currentThread()+":正在执行缓存重置");
                    R r = dbFallBack.apply(id);
                    RedisData flushData = new RedisData();
                    flushData.setData(r);
                    flushData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
                    redisTemplate.opsForValue().set(key,flushData);
                } finally {
                    unlock(lock_key);
                }
            });
        }
        return ((R) redisData.getData());
    }



    public boolean getLock(String key){
        Boolean isLock = redisTemplate.opsForValue().setIfAbsent(key, "1", 30, TimeUnit.SECONDS);
        return BooleanUtils.isTrue(isLock);
    }

    public void unlock(String key){
        redisTemplate.delete(key);
    }


}

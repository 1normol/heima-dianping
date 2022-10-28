package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SystemConstants;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.*;

import static com.hmdp.utils.SystemConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lml
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private RedisTemplate redisTemplate;


    @Override
    public Result getShopById(Long id) {
        Shop shop = getShopInfoLogicExpired(id);
        if (shop == null){
            return Result.fail("店铺不存在");
        }

        return Result.ok(shop);
    }

    /**
     * 根据id返回店铺信息(缓存空值解决缓存穿透)
     * 查询redis缓存,有信息-> 检查对象是否有属性值，若有,直接返回
     *                      若无，代表该缓存是缓存空值，直接返回失败。
     *              无信息->查询数据库->有数据,添加到redis缓存，并返回
     *                             ->无数据，设置缓存空值，TTL不宜设置太长。返回失败
     * @param id
     * @return
     */
    public Shop getShopInfoThroughCache(Long id) throws InterruptedException {
        Object o = redisTemplate.opsForValue().get(REDIS_CACHE_SHOP + id);
        if (ObjectUtils.isNotEmpty(o)){
            Shop cacheShop = (Shop) o;
            return cacheShop;
        }
        if (o != null){
            //说明对象走了缓存空值
            return null;
        }

        return getShop(id);
    }


    public Shop getShopInfoByMutex(Long id) {
        Object o = redisTemplate.opsForValue().get(REDIS_CACHE_SHOP + id);
        if (ObjectUtils.isNotEmpty(o)){
            Shop cacheShop = (Shop) o;
            return cacheShop;
        }
        if (o != null){
            //说明对象走了缓存空值
            return null;
        }
        //使用互斥锁重构缓存
        try {
            boolean isLock = getLock(REDIS_CACHE_SHOP_KEY + id);
            if (!isLock){
                Thread.sleep(50);
                return getShopInfoByMutex(id);
            }
            return getShop(id);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(REDIS_CACHE_SHOP_KEY + id);
        }


    }

    private Shop getShop(Long id) throws InterruptedException {
        Shop shop = query().eq("id", id).one();
        Thread.sleep(200);
        if (shop == null){
            redisTemplate.opsForValue().set(REDIS_CACHE_SHOP+id,"",2L, TimeUnit.MINUTES);
            return null;
        }
        redisTemplate.opsForValue().set(REDIS_CACHE_SHOP+id,shop,30L, TimeUnit.MINUTES);
        return shop;
    }
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
 //   ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10,10,200L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(10));
    //逻辑过期处理
    public Shop getShopInfoLogicExpired(Long id) {
        Object o = redisTemplate.opsForValue().get(REDIS_CACHE_LOGICAL_SHOP + id);
        if (ObjectUtils.isEmpty(o)){
            return null;
        }

        RedisData redisData = (RedisData) o;
        //如果还未过期,直接返回
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())){
            return (Shop) redisData.getData();
        }
        //过期，使用互斥锁，另起一个线程，异步重置缓存
        try {
            boolean isLock = getLock(REDIS_CACHE_SHOP_KEY + id);
            if (!isLock){
                Thread.sleep(50);
                return getShopInfoLogicExpired(id);
            }
            threadPool.submit(()->{
                System.out.println(Thread.currentThread()+":线程池异步刷新");
                setLogicExpireTimeToRedis(id,30L);
            });
            return (Shop) redisData.getData();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(REDIS_CACHE_SHOP_KEY+id);
        }
    }

    public void setLogicExpireTimeToRedis(Long id,Long expireTime){
        Shop shop = query().eq("id", id).one();
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        redisTemplate.opsForValue().set(REDIS_CACHE_LOGICAL_SHOP+id,redisData);
    }



    @Override
    public Result updateShopInfo(Shop shop) {
        updateById(shop);
        redisTemplate.delete(REDIS_CACHE_SHOP+shop.getId());
        return Result.ok();
    }

    public boolean getLock(String key){
        Boolean isLock = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtils.isTrue(isLock);
    }

    public void unlock(String key){
        redisTemplate.delete(key);
    }






}

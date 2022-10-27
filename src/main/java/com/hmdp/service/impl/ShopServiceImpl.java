package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SystemConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private RedisTemplate redisTemplate;
    @Override
    public Result getShopInfoById(Long id) {
        Shop cacheShop = (Shop) redisTemplate.opsForValue().get(SystemConstants.REDIS_CACHE_SHOP + id);
        if (cacheShop != null){
            return Result.ok(cacheShop);
        }
        Shop shop = query().eq("id", id).one();
        if (shop == null){
            return Result.fail("店铺不存在");
        }
        redisTemplate.opsForValue().set(SystemConstants.REDIS_CACHE_SHOP+id,shop);
        return Result.ok(shop);
    }
}

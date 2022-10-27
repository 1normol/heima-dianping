package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private RedisTemplate redisTemplate;
    @Override
    public Result getTypeList() {
        List shopList = redisTemplate.opsForList().range(SystemConstants.REDS_CACHE_SHOP_TYPE, 0, -1);
        if (!CollectionUtils.isEmpty(shopList)){

            return Result.ok(shopList);
        }
        List<ShopType> list = query().orderByAsc("sort").list();
        redisTemplate.opsForList().rightPushAll(SystemConstants.REDS_CACHE_SHOP_TYPE,list);
        return Result.ok(list);
    }
}

package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lml
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {
    /**
     * 根据id返回店铺信息(缓存空值解决缓存穿透)
     * 查询redis缓存,有信息-> 检查对象是否有属性值，若有,直接返回
     *                      若无，代表该缓存是缓存空值，直接返回失败。
     *              无信息->查询数据库->有数据,添加到redis缓存，并返回
     *                             ->无数据，设置缓存空值，TTL不宜设置太长。返回失败
     * @param id
     * @return
     */
    Result getShopById(Long id);






    Result updateShopInfo(Shop shop);
}

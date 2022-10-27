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
     * 根据id返回店铺信息
     * 查询redis缓存,有信息->直接返回。
     *              无数据->查询数据库->有数据,添加到redis缓存，并返回
     *                             ->无数据，返回失败
     * @param id
     * @return
     */
    Result getShopInfoById(Long id);
}

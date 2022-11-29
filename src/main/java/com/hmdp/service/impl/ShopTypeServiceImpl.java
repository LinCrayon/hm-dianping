package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

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
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result listShopType() {
        // 1.从redis中查询商铺类型缓存
        List<String> shopTypeJsons = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        // 2.判断是否命中缓存
        if (!CollectionUtils.isEmpty(shopTypeJsons)) {
            // 集合不为空即命中缓存 处理后返回
            // 定义集合 用于添加缓存中的数据
            List<ShopType> shopTypeList = new ArrayList<>();
            for (String shopTypeJson : shopTypeJsons) {
                // 遍历缓存反序列化后添加
                ShopType shopType = JSONUtil.toBean(shopTypeJson, ShopType.class);
                shopTypeList.add(shopType);
            }
            // 返回缓存数据
            return Result.ok(shopTypeList);
        }
        // 3.未命中缓存需查数据库
        List<ShopType> shopTypes = lambdaQuery().orderByAsc(ShopType::getSort).list();
        // 4.判断数据库中是否有数据
        if (CollectionUtils.isEmpty(shopTypes)) {
            // 不存在则缓存一个空集合 解决缓存穿透
            stringRedisTemplate.opsForValue()
                    .set(CACHE_SHOP_TYPE_KEY, Collections.emptyList().toString(), CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("商铺分类信息为空！");
        }
        // 数据存在 先写入缓存再返回
        // 定义集合 用于添加数据库中的数据
        List<String> shopTypeCache = new ArrayList<>();
        for (ShopType shopType : shopTypes) {
            // 遍历数据序列化为json后添加
            String jsonStr = JSONUtil.toJsonStr(shopType);
            shopTypeCache.add(jsonStr);
        }
        // 此处只能使用右插入 因为要保证顺序
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY, shopTypeCache);
        stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY, CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        // 返回数据库数据
        return Result.ok(shopTypes);
    }


    /*    @Override
    public Result getlist() {
        //1.在redis中查询
        String key = CACHE_TYPELIST_KEY;
        List<String> shopTypeList = stringRedisTemplate.opsForList().range(key, 0, -1);
        //2.判断是否缓存中了
        if (!shopTypeList.isEmpty()) {
        //3.中了返回
            ArrayList<Object> typeList  = new ArrayList<>();
            for (String s : shopTypeList) {
                ShopType shopType = JSONUtil.toBean(s, ShopType.class);
                typeList.add(shopType);
            }
            return Result.ok(typeList);
        }
        //查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //5.不存在直接返回错误
        if(typeList.isEmpty()){
            return Result.fail("不存在分类");
        }
        //存在遍历
        for(ShopType shopType : typeList){
            String s = JSONUtil.toJsonStr(shopType);
            shopTypeList.add(s);
        }
        //6.添加进缓存
        stringRedisTemplate.opsForList().rightPushAll(key, shopTypeList);
        return Result.ok(typeList);
    }*/
}

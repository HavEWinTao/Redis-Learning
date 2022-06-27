package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
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

    public Result queryTypeList() {
        // 1.从redis查询商铺类型缓存
        List<String> shopTypes = stringRedisTemplate.opsForList().range(CACHE_SHOPTYPE_KEY, 0, -1);
        // 2.判断是否存在
        if (shopTypes.size() != 0) {
            // 3.存在，直接返回
            List<ShopType> typeList = new ArrayList<>();
            shopTypes.forEach((json) -> {
                typeList.add(JSONUtil.toBean(json, ShopType.class));
            });
            return Result.ok(typeList);
        }
        //不存在，查数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList.size()==0) {
            return Result.fail("商户类型查询失败");
        }
        //写入redis
        typeList.forEach((shopType -> {
            stringRedisTemplate.opsForList().rightPush(CACHE_SHOPTYPE_KEY, JSONUtil.toJsonStr(shopType));
        }));
        //设置过期时间
        stringRedisTemplate.expire(CACHE_SHOPTYPE_KEY, CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);
        return Result.ok(typeList);
    }
}

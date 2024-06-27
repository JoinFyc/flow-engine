package com.wflow.utils;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;

import java.util.Objects;

/**
 * 缓存工具类，基于内存的简单超时数据缓存
 * @author : willian fu
 * @date : 2023/11/13
 */
public class WfCatchUtil {

    //全局数据缓存，默认存5分钟
    private static final TimedCache<String, Object> dataCatch = CacheUtil.newTimedCache(5*60000);

    static {
        //每20S自动清理一次缓存
        dataCatch.schedulePrune(20000);
    }

    public static <T> T getCatch(String key, Class<T> clazz){
        Object data = dataCatch.get(key);
        if (Objects.nonNull(data)){
            return clazz.cast(data);
        }
        return null;
    }

    public static void putCatch(String key, Object value, long timeout){
        dataCatch.put(key, value, timeout);
    }
}

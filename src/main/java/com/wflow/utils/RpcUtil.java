package com.wflow.utils;

import com.wflow.bean.do_.LoginDo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * @author JoinFyc
 * @version : 1.0
 * @description Rpc用户登录信息
 * @date 2024 09:09
 */
public class RpcUtil {

    /**
     * 获取当前用户租户ID
     * @return LoginDo 租户ID
     */
    public static LoginDo getLoginTenant() {
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElseThrow(() -> new IllegalArgumentException("租户ID[tenantId]不能为空"));
        String userId = Optional.ofNullable(request.getHeader("userId")).orElseThrow(() -> new IllegalArgumentException("用户ID[userId]不能为空"));
        return LoginDo.builder().tenantId(tenantId).userId(userId).build();
    }
}

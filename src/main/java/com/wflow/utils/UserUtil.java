package com.wflow.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.wflow.bean.FlowProcessContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * @author : willian fu
 * @date : 2022/9/29
 */
public class UserUtil {

    /**
     * 获取当前登录用户的id
     *
     * @return 用户ID
     */
    public static String getLoginUserId() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            final FlowProcessContext flowProcessContext = FlowProcessContext.getFlowProcessContext();
            if (flowProcessContext != null && flowProcessContext.getFieldTag() == Boolean.TRUE) {
                return flowProcessContext.getUserId();
            }else {
                throw e;
            }
        }
    }

    /**
     * 获取当前用户租户ID
     *
     * @return 租户ID
     */
    public static String getLoginTenantId() {
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        return Optional.ofNullable(request.getHeader("TenantId")).orElse("default");
    }
}

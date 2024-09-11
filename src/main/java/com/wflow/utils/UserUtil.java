package com.wflow.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.wflow.bean.FlowProcessContext;
import com.wflow.bean.do_.LoginDo;
import org.junit.jupiter.params.aggregator.ArgumentAccessException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.NoSuchElementException;
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

}

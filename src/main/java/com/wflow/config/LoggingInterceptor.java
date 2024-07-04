package com.wflow.config;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author JoinFyc
 * @description 日志拦截器
 * @date 2024-07-03
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    private final String ignoreUrl = "/wflow/notify";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!request.getRequestURI().contains(ignoreUrl)) {
            logger.info("请求路径:{},请求方法:{},请求参数:{}", request.getRequestURL().toString(), request.getMethod(), JSONObject.toJSONString(request.getParameterMap()));
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (!request.getRequestURI().contains(ignoreUrl)) {
            logger.info("响应状态:{}", response.getStatus());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null) {
            logger.error("异常:", ex);
        }
    }
}


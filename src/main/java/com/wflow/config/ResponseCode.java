package com.wflow.config;

/**
 * @author : willian fu
 * @date : 2022/8/15
 */
public interface ResponseCode {

    int SUCCESS = 0;
    //参数异常、缺失，操作失败
    int FAIL = 400;
    //登录过期、未登录
    int EXPIRE = 401;
    //禁止访问
    int FORBIDDEN = 403;
    //服务器异常
    int ERROR = 500;
}

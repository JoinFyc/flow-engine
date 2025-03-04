package com.wflow.exception;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author : JoinFyc
 * @date : 2024/6/27
 */
@AllArgsConstructor
public enum ResultCode {

    LOGIN_USER_NOTFOUND(400, "用户不存在"),

    LOGIN_USER_FAIL(400, "用户名或密码错误");

    @Getter
    private Integer code;

    @Getter
    private String msg;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}

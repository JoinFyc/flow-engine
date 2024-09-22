package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : JoinFyc
 * @date : 2024/8/15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseVo<T> {
    //响应码
    private Integer code;
    //消息
    private String msg;
    //错误消息
    private T data;

}

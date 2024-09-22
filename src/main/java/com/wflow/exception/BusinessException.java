package com.wflow.exception;

import lombok.Getter;

/**
 * @author : JoinFyc
 * @date : 2024/6/27
 */
@Getter
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}



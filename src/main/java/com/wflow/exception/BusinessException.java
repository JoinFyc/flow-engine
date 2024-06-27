package com.wflow.exception;

import lombok.Getter;

/**
 * @author : willian fu
 * @date : 2022/6/27
 */
@Getter
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}



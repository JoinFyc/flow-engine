package com.wflow.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.util.StrUtil;
import com.wflow.bean.vo.ResponseVo;
import com.wflow.exception.BusinessException;
import com.wflow.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

/**
 * @author : willian fu
 * @date : 2022/6/27
 */
@Slf4j
@Component
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> businessExceptionHandler(BusinessException e){
        log.error("BusinessException：", e);
        return R.serverError(e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<String> LoginExceptionHandler(NotLoginException e){
        log.error("NotLoginException：", e);
        return R.unAuthorized("未登录或登录Token已过期，请点击头像切换人员");
    }

    @ExceptionHandler(FlowableException.class)
    public ResponseEntity<String> flowableExceptionHandler(FlowableException e){
        log.error("FlowableException：", e);
        String error = "流程执行异常: " + e.getMessage();
        if (StrUtil.isNotBlank(e.getMessage())){
            if (e.getMessage().contains("No outgoing sequence")){
                error = "下方条件分支所有条件都不满足，流程无法继续执行，请检查";
            } else if (e.getMessage().contains("Couldn't deserialize object in variable")) {
                error = "流程变量[ " + e.getMessage().substring(40) + " ]无法反序列化成对象，请检查";
            }
            //其他情况继续加else
        }
        return R.serverError(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> runtimeExceptionHandler(Exception e){
        log.error("RuntimeException：", e);
        return R.serverError(e.getMessage());
    }
}

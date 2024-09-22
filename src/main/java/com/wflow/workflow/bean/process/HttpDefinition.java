package com.wflow.workflow.bean.process;

import cn.hutool.http.Method;
import lombok.Data;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2023/10/28
 */
@Data
public class HttpDefinition {

    private String url;
    private Method method;
    private List<KeyValue> headers;
    private String contentType;
    private List<KeyValue> params;
    private Object data;
    private String preHandler;
    private String aftHandler;

    @Data
    public static class KeyValue {
        private String name;

        private String value;
    }
}

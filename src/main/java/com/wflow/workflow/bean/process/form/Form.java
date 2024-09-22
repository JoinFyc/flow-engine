package com.wflow.workflow.bean.process.form;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/7/7
 */
@Data
public class Form implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;

    private String id;
    private String icon;
    private String name;
    private Map<String, Object> props;
    private String title;
    private Object value;
    private String key; //自定义表单字段key值
    private ValueType valueType;
    private FormPerm.PermEnum perm;
}

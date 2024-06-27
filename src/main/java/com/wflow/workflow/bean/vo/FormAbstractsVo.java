package com.wflow.workflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : willian fu
 * @date : 2024/2/25
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormAbstractsVo {
    //表单字段ID
    private String id;
    //表单字段名称
    private String name;
    //表单字段key
    private String key;
    //表单字段类型
    private String type;
    //表单字段值
    private Object value;
}

package com.wflow.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程表单数据表(WflowFormData)实体类
 *
 * @author makejava
 * @since 2024-02-22 14:43:46
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WflowFormData implements Serializable {
    private static final long serialVersionUID = -79867098943068371L;
    /**
     * 主键
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 流程实例id
     */
    private String instanceId;
    /**
     * 该流程版本
     */
    private Integer version;
    /**
     * 流程编号
     */
    private String code;
    /**
     * 流程定义ID
     */
    private String defId;
    /**
     * 表单字段ID
     */
    private String fieldId;
    /**
     * 表单字段key
     */
    private String fieldKey;
    /**
     * 表单字段名称
     */
    private String fieldName;
    /**
     * 表单字段组件类型
     */
    private String fieldType;
    /**
     * 字段值是否为json
     */
    private Boolean isJson;
    /**
     * 表单字段值
     */
    private String fieldValue;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}


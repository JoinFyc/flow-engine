package com.wflow.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.io.Serializable;

/**
 * 表单字段版本修改记录表(WflowFormRecord)实体类
 *
 * @author makejava
 * @since 2024-02-22 14:44:14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WflowFormRecord implements Serializable {
    private static final long serialVersionUID = 380257788131918352L;
    /**
     * 主键
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 流程实例ID
     */
    private String instanceId;
    /**
     * 字段id
     */
    private String fieldId;
    /**
     * 旧的值
     */
    private String oldValue;
    /**
     * 新的值
     */
    private String newValue;
    /**
     * 修改的时间
     */
    private Date createTime;
    /**
     * 修改人ID
     */
    private String updateBy;

}


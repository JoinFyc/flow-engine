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
 * 子流程设计表(WflowSubProcess)实体类
 *
 * @author makejava
 * @since 2023-11-27 11:56:41
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WflowSubProcess implements Serializable {
    private static final long serialVersionUID = -40350862147061027L;
    /**
     * 子流程id
     */
    @TableId(type = IdType.INPUT)
    private String id;

    private String procCode;
    /**
     * 子流程定义id
     */
    private String procDefId;

    private String  deployId;
    /**
     * 子流程设计json
     */
    private String process;
    /**
     * 子流程名称
     */
    private String procName;
    /**
     * 子流程版本号
     */
    private Integer version;
    /**
     * 分组id
     */
    private Long groupId;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 是否已停用
     */
    private Boolean isStop;
    /**
     * 已删除
     */
    private Boolean isDeleted;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建时间
     */
    private Date created;
    /**
     * 更新时间
     */
    private Date updated;

}


package com.wflow.bean.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WflowModels implements Serializable {
    private static final long serialVersionUID = -40467384325438214L;

    /**
    * 表单ID
    */
    @TableId(type = IdType.INPUT)
    private String formId;

    private String processDefId;

    private String deployId;
    //流程版本
    private Integer version;
    /**
    * 表单名称
    */
    private String formName;
    /**
    * 图标配置
    */
    private String logo;
    /**
    * 设置项
    */
    private String settings;
    /**
    * 分组ID
    */
    private Long groupId;
    /**
    * 表单设置内容
    */
    private String formItems;
    //表单配置项
    private String formConfig;
    //表单摘要字段信息
    private String formAbstracts;
    /**
    * 流程设置内容
    */
    private String process;

    private String processConfig;
    /**
    * 备注
    */
    private String remark;
    /**
     * 状态 0=正常 1=已停用 2=已删除
     */
    private Boolean isStop;

    private Boolean isDelete;

    private Integer sort;
    /**
    * 创建时间
    */
    private Date created;
    /**
    * 更新时间
    */
    private Date updated;

    /**
     * 业务事件Key
     */
    private String businessEventKey;



}

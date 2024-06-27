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
public class WflowProcessInstances implements Serializable {
    private static final long serialVersionUID = 158464951078025287L;

    @TableId(type = IdType.AUTO)
    /**
    * 流程实例ID
    */
    private String instanceId;
    /**
    * 发起人
    */
    private String owner;
    /**
    * 发起部门
    */
    private String deptId;
    /**
    * 流程模型编码
    */
    private String code;
    /**
    * 流程模型版本
    */
    private Integer modelVer;
    /**
    * 业务编号
    */
    private String businessId;
    /**
    * REFUSE  AGRRE
    */
    private String result;
    /**
    * 表单数据
    */
    private String formData;
    /**
    * 流程状态  RUNNING=进行中 COMPLETE=已完成  CANCEL=已取消
    */
    private String status;
    /**
    * 创建时间
    */
    private Date createTime;
    /**
    * 更新时间
    */
    private Date updateTime;
    /**
    * 结束时间
    */
    private Date finishTime;


}

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
public class WflowModelHistorys implements Serializable {
    private static final long serialVersionUID = 478762346225421748L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String processDefId;

    private String deployId;

    private String formId;

    private String formName;

    private Integer version;

    private String settings;

    private Long groupId;

    private String process;

    private String processConfig;

    private String remark;

    private Date created;

    private String formItems;

    private String formAbstracts;

    private String formConfig;

    private String logo;

    private String businessEventKey;

    /**
     * 表单类型
     */
    private String formType;
}

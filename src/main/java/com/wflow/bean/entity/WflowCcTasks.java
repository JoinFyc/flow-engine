package com.wflow.bean.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WflowCcTasks implements Serializable {
    private static final long serialVersionUID = -80075781855060928L;

    @TableId(type = IdType.ASSIGN_ID)

    private Long id;
    /**
    * 审批实例ID
    */
    @Getter
    private String instanceId;
    /**
    * 抄送用户
    */
    private String userId;
    /**
    * 抄送节点ID
    */
    private String nodeId;

    /**
     * 审批模板编号
     */
    private String code;
    /**
    * 抄送节点名称
    */
    private String nodeName;
    /**
    * 抄送时间
    */
    private Date createTime;


}

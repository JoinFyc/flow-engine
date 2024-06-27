package com.wflow.workflow.bean.vo;

import com.wflow.workflow.bean.process.OrgUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : willian fu
 * @date : 2022/8/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessTaskVo {
    //任务ID
    private String taskId;
    //任务定义key
    private String taskDefKey;
    //流程定义ID
    private String processDefId;
    //流程执行ID
    private String executionId;
    //任务名称
    private String taskName;
    //任务归属节点
    private String nodeId;

    //任务处理结果
    private String taskResult;

    //部署ID
    private String deployId;
    //流程定义名称
    private String processDefName;
    //版本
    private Integer version;
    //实例ID
    private String instanceId;
    //父级流程实例ID
    private String superInstanceId;

    //流程发起人
    private String ownerId;
    private OrgUser owner;
    //流程发起人部门ID
    private String ownerDeptId;
    //流程发起人部门名称
    private String ownerDeptName;
    //表单数据摘要信息
    private List<FormAbstractsVo> formAbstracts;
    //流程实例创建时间
    private Date createTime;
    //task创建时间
    private Date taskCreateTime;
    //task完成时间
    private Date taskEndTime;

}

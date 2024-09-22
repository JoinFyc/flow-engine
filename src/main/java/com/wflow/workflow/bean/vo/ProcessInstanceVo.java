package com.wflow.workflow.bean.vo;

import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.enums.ProcessResultEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/9/4
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessInstanceVo {

    //部署ID
    private String deployId;
    //流程定义名称
    private String processDefName;
    //流程实例名称
    private String instanceName;
    //版本
    private Integer version;
    //模型表单定义ID
    private String formId;
    //实例ID
    private String instanceId;
    //流程定义ID
    private String processDefId;
    //父流程id
    private String superInstanceId;
    //任务关联的流程节点ID
    private String nodeId;
    //任务名称
    private String taskName;
    //流程状态
    private String status;
    //流程结果
    private ProcessResultEnum result;
    //发起人信息
    private String staterUserId;
    private OrgUser staterUser;
    //表单数据摘要信息
    private List<FormAbstractsVo> formAbstracts;
    //流程实例创建时间
    private Date startTime;
    private Date finishTime;
}

package com.wflow.workflow.bean.vo;

import com.alibaba.fastjson2.JSONObject;
import com.wflow.workflow.bean.process.OperationPerm;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.enums.ApprovalModeEnum;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.bean.process.enums.ProcessResultEnum;
import com.wflow.workflow.bean.process.form.Form;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/9/5
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessProgressVo {

    //流程唯一业务ID
    private String flowUniqueId;
    //最后一个审批人标识
    private Boolean lastAudit = Boolean.FALSE;
    //审批实例ID
    private String instanceId;
    //表单配置项
    private List<Form> formItems;
    //表单规则
    private JSONObject formConfig;
    //表单值
    private Map<String, Object> formData;
    //流程进度步骤
    private List<ProgressNode> progress;
    //流程定义名称
    private String processDefName;
    //版本
    private Integer version;
    //流程状态
    private String status;
    //流程结果
    private ProcessResultEnum result;
    //流程按钮权限配置
    private OperationPerm operationPerm;
    //发起人
    private OrgUser staterUser;
    //发起人部门
    private String starterDept;
    //发起时间
    private Date startTime;
    //结束时间
    private Date finishTime;
    //扩展设置
    private InstanceExternSetting externSetting;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InstanceExternSetting {
        //允许撤销
        private Boolean enableCancel;
        //审批签字设置
        private Boolean enableSign;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressNode {
        //是否为未来节点
        private Boolean isFuture;
        //节点ID
        private String nodeId;
        //任务ID
        private String taskId;
        //审批类型
        private ApprovalModeEnum approvalMode;
        //节点类型
        private NodeTypeEnum nodeType;
        //节点名称
        private String name;
        //节点相关人员
        private OrgUser user;
        //上一个任务人，转交之前的人，只保留前一个
        private OrgUser owner;
        //该节点动作操作类型
        private ProcessHandlerParamsVo.Action action;
        //签字
        private String signature;
        //处理意见
        private List<TaskCommentVo> comment;
        //private List<>
        //处理结果
        private ProcessHandlerParamsVo.Action result;
        //开始结束时间
        private Date startTime;
        private Date finishTime;
    }
}

package com.wflow.workflow.task;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wflow.utils.BeanUtil;
import com.wflow.workflow.bean.dto.NotifyDto;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.bean.vo.ProcessHandlerParamsVo;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.service.NotifyService;
import com.wflow.workflow.service.ProcessNodeCatchService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.history.HistoricProcessInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 审批超时服务任务
 *
 * @author : willian fu
 * @date : 2022/9/12
 */
@Slf4j
public class ApprovalTimeoutServiceTask implements JavaDelegate {

    private static TaskService taskService;

    private static HistoryService historyService;

    private static NotifyService notifyService;

    private static ProcessNodeCatchService nodeCatchService;

    public ApprovalTimeoutServiceTask() {
        taskService = BeanUtil.getBean(TaskService.class);
        historyService = BeanUtil.getBean(HistoryService.class);
        notifyService = BeanUtil.getBean(NotifyService.class);
        nodeCatchService = BeanUtil.getBean(ProcessNodeCatchService.class);
    }

    @Override
    public void execute(DelegateExecution execution) {
        //执行审批超期逻辑
        FlowElement element = execution.getCurrentFlowElement();
        String[] split = element.getId().split("-");
        Map variable = execution.getVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS, Map.class);
        ApprovalProps props = (ApprovalProps) variable.get(split[0]);
        ApprovalProps.TimeLimit timeLimit = props.getTimeLimit();
        switch (timeLimit.getHandler().getType()) {
            case PASS: //自动通过处理任务
                ProcessNode<?> node = nodeCatchService.getProcessNode(execution.getProcessDefinitionId(), split[0]);
                handlerApprovalTask(execution.getProcessInstanceId(), split[0],
                        NodeTypeEnum.TASK.equals(node.getType()) ? ProcessHandlerParamsVo.Action.complete: ProcessHandlerParamsVo.Action.agree);
                break;
            case NOTIFY: //发送通知
                sendNotify(execution);
                break;
            default: //自动代替审批人处理拒绝审批
                handlerApprovalTask(execution.getProcessInstanceId(), split[0], ProcessHandlerParamsVo.Action.refuse);
                break;
        }
    }

    /**
     * 系统自动处理审批任务
     *
     * @param instanceId 实例ID
     * @param action     处理动作
     */
    private void handlerApprovalTask(String instanceId, String nodeId, ProcessHandlerParamsVo.Action action) {
        taskService.createTaskQuery().processInstanceId(instanceId)
                .taskDefinitionKey(nodeId).active().list().forEach(task -> {
                    try {
                        String assignee = task.getAssignee();
                        Authentication.setAuthenticatedUserId(assignee);
                        Map<String, Object> var = new HashMap<>();
                        var.put(WflowGlobalVarDef.TASK_RES_PRE + task.getId(), action);
                        taskService.complete(task.getId(), var);
                        log.info("审批实例[{}] 节点[{}] 审批人[{}]处理[{}]超时, 自动{}", instanceId, task.getTaskDefinitionKey(), assignee, task.getId(), action);
                        taskService.addComment(task.getId(), instanceId, JSONObject.toJSONString(new ProcessHandlerParamsVo.ProcessComment(
                                (ProcessHandlerParamsVo.Action.complete.equals(action) ? "办理" : "审批") + "超时，系统自动处理", Collections.emptyList())));
                        Authentication.setAuthenticatedUserId(null);
                    } catch (Exception ignored) {}
                });

    }

    /**
     * 发送消息通知
     *
     * @param execution 执行实例
     */
    private void sendNotify(DelegateExecution execution) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(execution.getProcessInstanceId()).singleResult();
        historyService.createHistoricTaskInstanceQuery().processInstanceId(execution.getProcessInstanceId())
                .unfinished().list().forEach(task -> {
                    String assignee = task.getAssignee();
                    notifyService.notify(NotifyDto.builder()
                            .title("审批超时提醒")
                            .processDefId(execution.getProcessDefinitionId())
                            .instanceId(execution.getProcessInstanceId())
                            .target(assignee)
                            .content(StrUtil.builder("您有一项【",
                                    instance.getProcessDefinitionName(),
                                    "】审批任务已超时，请即时处理").toString())
                            .type(NotifyDto.TypeEnum.WARNING)
                            .build());
                    log.info("审批[{}]超时催办通知，您[{}]有一条审批任务[{}]等待处理", instance.getId(), assignee, task.getName());
                });
    }
}

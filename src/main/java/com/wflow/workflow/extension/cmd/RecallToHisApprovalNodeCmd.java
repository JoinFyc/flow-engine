package com.wflow.workflow.extension.cmd;

import cn.hutool.core.collection.CollectionUtil;
import com.wflow.workflow.utils.FlowableUtils;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : JoinFyc
 * @date : 2024/08/14
 */
public class RecallToHisApprovalNodeCmd implements Command<String>, Serializable {

    private static final long serialVersionUID = -80075781855060928L;

    protected RuntimeService runtimeService;
    protected String taskId;
    protected String targetNodeId;

    public RecallToHisApprovalNodeCmd(RuntimeService runtimeService, String taskId, String targetNodeId) {
        this.runtimeService = runtimeService;
        this.taskId = taskId;
        this.targetNodeId = targetNodeId;
    }

    @Override
    public String execute(CommandContext commandContext) {
        if (targetNodeId == null || targetNodeId.length() == 0) {
            throw new FlowableException("退回的目标节点不能为空");
        }
        TaskEntity task = CommandContextUtil.getProcessEngineConfiguration().getTaskServiceConfiguration().getTaskService().getTask(taskId);
        if (task == null) {
            throw new FlowableObjectNotFoundException(taskId + " 任务不能为空", Task.class);
        }
        String sourceNodeId = task.getTaskDefinitionKey();
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();
        //获取流程定义
        Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        FlowNode sourceFlowElement = (FlowNode) process.getFlowElement(sourceNodeId, true);
        // 只支持从用户任务退回
        if (!(sourceFlowElement instanceof UserTask)) {
            throw new FlowableException("只能从审批节点进行回退");
        }
        FlowNode targetFlowElement = (FlowNode) process.getFlowElement(targetNodeId, true);
        // 退回节点到当前节点不可达到，不允许退回
        if (!ExecutionGraphUtil.isReachable(processDefinitionId, targetNodeId, sourceNodeId)) {
            throw new FlowableException("无法回退到目标节点");
        }
        //目标节点如果相对当前节点是在子流程内部，则无法直接退回，目前处理是只能退回到子流程开始节点
        String[] sourceAndTargetRealActivityId = FlowableUtils.getSourceAndTargetRealActivityId(sourceFlowElement, targetFlowElement);
        // 实际应操作的当前节点ID
        String sourceRealActivityId = sourceAndTargetRealActivityId[0];
        // 实际应操作的目标节点ID
        String targetRealActivityId = sourceAndTargetRealActivityId[1];

        Map<String, Set<String>> specialGatewayNodes = FlowableUtils.getSpecialGatewayElements(process);
        // 当前节点处在的并行网关list
        List<String> sourceInSpecialGatewayList = new ArrayList<>();
        // 目标节点处在的并行网关list
        List<String> targetInSpecialGatewayList = new ArrayList<>();
        setSpecialGatewayList(sourceRealActivityId, targetRealActivityId, specialGatewayNodes, sourceInSpecialGatewayList, targetInSpecialGatewayList);
        // 实际应筛选的节点ID
        Set<String> sourceRealAcitivtyIds = null;
        // 若退回目标节点相对当前节点在并行网关中，则要找到相对当前节点最近的这个并行网关，后续做特殊处理
        String targetRealSpecialGateway = null;

        // 1.目标节点和当前节点都不在并行网关中
        if (targetInSpecialGatewayList.isEmpty() && sourceInSpecialGatewayList.isEmpty()) {
            sourceRealAcitivtyIds = CollectionUtil.newLinkedHashSet(sourceRealActivityId);
        }
        // 2.目标节点不在并行网关中、当前节点在并行网关中
        else if (targetInSpecialGatewayList.isEmpty()) {
            sourceRealAcitivtyIds = specialGatewayNodes.get(sourceInSpecialGatewayList.get(0));
        }
        // 3.目标节点在并行网关中、当前节点不在并行网关中
        else if (sourceInSpecialGatewayList.isEmpty()) {
            sourceRealAcitivtyIds = CollectionUtil.newLinkedHashSet(sourceRealActivityId);
            targetRealSpecialGateway = targetInSpecialGatewayList.get(0);
        }
        // 4.目标节点和当前节点都在并行网关中
        else {
            int diffSpecialGatewayLevel = FlowableUtils.getDiffLevel(sourceInSpecialGatewayList, targetInSpecialGatewayList);
            // 在并行网关同一层且在同一分支
            if (diffSpecialGatewayLevel == -1) {
                sourceRealAcitivtyIds = CollectionUtil.newLinkedHashSet(sourceRealActivityId);
            } else {
                // 当前节点最内层并行网关不被目标节点最内层并行网关包含
                // 或理解为当前节点相对目标节点在并行网关外
                // 只筛选当前节点的execution
                if (sourceInSpecialGatewayList.size() == diffSpecialGatewayLevel) {
                    sourceRealAcitivtyIds = CollectionUtil.newLinkedHashSet(sourceRealActivityId);
                }
                // 当前节点相对目标节点在并行网关内，应筛选相对目标节点最近的并行网关的所有节点的execution
                else {
                    sourceRealAcitivtyIds = specialGatewayNodes.get(sourceInSpecialGatewayList.get(diffSpecialGatewayLevel));
                }
                // 目标节点最内层并行网关包含当前节点最内层并行网关
                // 或理解为目标节点相对当前节点在并行网关外
                // 不做处理
                if (targetInSpecialGatewayList.size() == diffSpecialGatewayLevel) {
                }
                // 目标节点相对当前节点在并行网关内
                else {
                    targetRealSpecialGateway = targetInSpecialGatewayList.get(diffSpecialGatewayLevel);
                }
            }
        }

        // 筛选需要处理的execution
        List<ExecutionEntity> realExecutions = this.getRealExecutions(commandContext, processInstanceId,
                task.getExecutionId(), sourceRealActivityId, sourceRealAcitivtyIds);
        // 执行退回，直接跳转到实际的 targetRealActivityId
        List<String> realExecutionIds = realExecutions.stream().map(ExecutionEntity::getId).collect(Collectors.toList());
        runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId).moveExecutionsToSingleActivityId(realExecutionIds, targetRealActivityId).changeState();
        // 目标节点相对当前节点处于并行网关内，需要特殊处理，需要手动生成并行网关汇聚节点(_end)的execution数据
        if (targetRealSpecialGateway != null) {
            createTargetInSpecialGatewayEndExecutions(commandContext, realExecutions, process, targetInSpecialGatewayList, targetRealSpecialGateway);
        }
        return targetRealActivityId;
    }

    private void setSpecialGatewayList(String sourceNodeId, String targetNodeId, Map<String, Set<String>> specialGatewayNodes,
                                       List<String> sourceInSpecialGatewayList, List<String> targetInSpecialGatewayList) {
        for (Map.Entry<String, Set<String>> entry : specialGatewayNodes.entrySet()) {
            if (entry.getValue().contains(sourceNodeId)) {
                sourceInSpecialGatewayList.add(entry.getKey());
            }
            if (entry.getValue().contains(targetNodeId)) {
                targetInSpecialGatewayList.add(entry.getKey());
            }
        }
    }

    private void createTargetInSpecialGatewayEndExecutions(CommandContext commandContext, List<ExecutionEntity> excutionEntitys, Process process,
                                                           List<String> targetInSpecialGatewayList, String targetRealSpecialGateway) {
        // 目标节点相对当前节点处于并行网关，需要手动生成并行网关汇聚节点(_end)的execution数据
        String parentExecutionId = excutionEntitys.iterator().next().getParentId();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity parentExecutionEntity = executionEntityManager.findById(parentExecutionId);

        int index = targetInSpecialGatewayList.indexOf(targetRealSpecialGateway);
        for (; index < targetInSpecialGatewayList.size(); index++) {
            String targetInSpecialGateway = targetInSpecialGatewayList.get(index);
            FlowNode targetInSpecialGatewayEnd = (FlowNode) process.getFlowElement(targetInSpecialGateway + "_end", true);
            int nbrOfExecutionsToJoin = targetInSpecialGatewayEnd.getIncomingFlows().size();
            // 处理目标节点所处的分支以外的分支,即 总分枝数-1 = nbrOfExecutionsToJoin - 1
            for (int i = 0; i < nbrOfExecutionsToJoin - 1; i++) {
                ExecutionEntity childExecution = executionEntityManager.createChildExecution(parentExecutionEntity);
                childExecution.setCurrentFlowElement(targetInSpecialGatewayEnd);
                ActivityBehavior activityBehavior = (ActivityBehavior) targetInSpecialGatewayEnd.getBehavior();
                activityBehavior.execute(childExecution);
            }
        }
    }

    private List<ExecutionEntity> getRealExecutions(CommandContext commandContext, String processInstanceId,
                                                    String taskExecutionId, String sourceRealActivityId, Set<String> activityIds) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity taskExecution = executionEntityManager.findById(taskExecutionId);
        List<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        Set<String> parentExecutionIds = FlowableUtils.getParentExecutionIdsByActivityId(executions, sourceRealActivityId);
        String realParentExecutionId = FlowableUtils.getParentExecutionIdFromParentIds(taskExecution, parentExecutionIds);
        return executionEntityManager.findExecutionsByParentExecutionAndActivityIds(realParentExecutionId, activityIds);
    }
}

package com.wflow.workflow.utils;

import cn.hutool.core.util.StrUtil;
import com.wflow.utils.SpringContextUtil;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.config.WflowGlobalVarDef;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author wliianfu
 * @date 2024/08/14
 */
public class FlowableUtils {

    public static Map<String, Set<String>> getSpecialGatewayElements(FlowElementsContainer container) {
        return getSpecialGatewayElements(container, null);
    }

    /**
     * 批量获取流程变量
     * @param instanceIds 流程实例ID
     * @param varName 变量名
     * @return 实例ID -> 流程变量值
     */
    public static Map<String, Object> getProcessVars(Collection<String> instanceIds, String varName) {
        HistoryService historyService = SpringContextUtil.getBean(HistoryService.class);
        return historyService.createNativeHistoricVariableInstanceQuery()
                .sql(StrUtil.builder().append("select * from ACT_HI_VARINST where PROC_INST_ID_ IN ('")
                        .append(String.join("','", instanceIds)).append("')")
                        .append(" and NAME_ = #{name}").toString())
                .parameter("name", varName)
                .list().stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getProcessInstanceId, HistoricVariableInstance::getValue));
    }

    /**
     * 批量获取流程变量
     * @param instanceIds 流程实例ID
     * @param varNames 变量名集合
     * @return 实例ID+变量名 -> 流程变量值
     */
    public static Map<String, Object> getProcessVars(Collection<String> instanceIds, Collection<String> varNames) {
        HistoryService historyService = SpringContextUtil.getBean(HistoryService.class);
        return historyService.createNativeHistoricVariableInstanceQuery()
                .sql(StrUtil.builder().append("select * from ACT_HI_VARINST where PROC_INST_ID_ IN ('")
                        .append(String.join("','", instanceIds)).append("')")
                        .append(" and NAME_ IN ('")
                        .append(String.join("','", varNames)).append("')")
                        .toString())
                .list().stream()
                .collect(Collectors.toMap(v -> v.getProcessInstanceId() + v.getVariableName(), HistoricVariableInstance::getValue));
    }

    /**
     * 查询部门ID，之前的逻辑数据，后续已经变更为部门ID
     * @param instanceId 实例ID
     * @param runtime 是否是运行时
     * @return 部门ID
     */
    public static String getOwnerDept(String instanceId, boolean runtime) {
        if (runtime){
            RuntimeService runtimeService = SpringContextUtil.getBean(RuntimeService.class);
            ProcessInstanceOwnerDto owner = (ProcessInstanceOwnerDto) runtimeService.getVariable(instanceId, WflowGlobalVarDef.OWNER);
            return Optional.ofNullable(owner)
                    .orElseGet(ProcessInstanceOwnerDto::new)
                    .getOwnerDeptId();
        }
        HistoryService historyService = SpringContextUtil.getBean(HistoryService.class);
        HistoricVariableInstance result = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instanceId).variableName(WflowGlobalVarDef.OWNER).singleResult();
        if (Objects.nonNull(result)) {
            return Optional.ofNullable((ProcessInstanceOwnerDto) result.getValue())
                    .orElseGet(ProcessInstanceOwnerDto::new).getOwnerDeptId();
        }
        return null;
    }

    /**
     * 搜索特殊的网关，主要是并行网关
     * @param container 节点容器
     * @param specialGatewayElements 搜索到的
     * @return 搜索到的
     */
    public static Map<String, Set<String>> getSpecialGatewayElements(FlowElementsContainer container, Map<String, Set<String>> specialGatewayElements) {
        if (specialGatewayElements == null) {
            specialGatewayElements = new HashMap<>(16);
        }
        Collection<FlowElement> flowElements = container.getFlowElements();
        for (FlowElement flowElement : flowElements) {
            boolean isBeginSpecialGateway = flowElement.getId().endsWith("_begin")
                    && (flowElement instanceof ParallelGateway
                    || flowElement instanceof InclusiveGateway
                    || flowElement instanceof ComplexGateway);
            if (isBeginSpecialGateway) {
                String gatewayBeginRealId = flowElement.getId();
                String gatewayId = gatewayBeginRealId.substring(0, gatewayBeginRealId.length() - 6);
                Set<String> gatewayIdContainFlowElements = specialGatewayElements.computeIfAbsent(gatewayId, k -> new HashSet<>());
                findElementsBetweenSpecialGateway(flowElement, gatewayId + "_end", gatewayIdContainFlowElements);
            } else if (flowElement instanceof SubProcess) {
                getSpecialGatewayElements((SubProcess) flowElement, specialGatewayElements);
            }
        }

        // 外层到里层排序
        Map<String, Set<String>> specialGatewayNodesSort = new LinkedHashMap<>();
        specialGatewayElements.entrySet().stream().sorted((o1, o2) -> o2.getValue().size() - o1.getValue().size()).forEach(entry -> specialGatewayNodesSort.put(entry.getKey(), entry.getValue()));
        return specialGatewayNodesSort;
    }

    public static void findElementsBetweenSpecialGateway(FlowElement specialGatewayBegin, String specialGatewayEndId, Set<String> elements) {
        elements.add(specialGatewayBegin.getId());
        List<SequenceFlow> sequenceFlows = ((FlowNode) specialGatewayBegin).getOutgoingFlows();
        if (sequenceFlows != null && sequenceFlows.size() > 0) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
                String targetFlowElementId = targetFlowElement.getId();
                elements.add(specialGatewayEndId);
                if (!targetFlowElementId.equals(specialGatewayEndId)) {
                    findElementsBetweenSpecialGateway(targetFlowElement, specialGatewayEndId, elements);
                }
            }
        }
    }

    /**
     * 查询节点所有父级
     * @param flowNode 被查询的节点
     * @return 父级节点集合
     */
    public static List<String> getParentProcessIds(FlowNode flowNode) {
        List<String> result = new ArrayList<>();
        FlowElementsContainer flowElementsContainer = flowNode.getParentContainer();
        while (flowElementsContainer != null) {
            if (flowElementsContainer instanceof SubProcess) {
                SubProcess flowElement = (SubProcess) flowElementsContainer;
                result.add(flowElement.getId());
                flowElementsContainer = flowElement.getParentContainer();
            } else if (flowElementsContainer instanceof Process) {
                Process flowElement = (Process) flowElementsContainer;
                result.add(flowElement.getId());
                flowElementsContainer = null;
            }
        }
        // 第一层Process为第0个
        Collections.reverse(result);
        return result;
    }

    /**
     * 查询不同层级
     * @param sourceList 源集合
     * @param targetList 目标集合
     * @return 层级
     */
    public static Integer getDiffLevel(List<String> sourceList, List<String> targetList) {
        if (sourceList == null || sourceList.isEmpty() || targetList == null || targetList.isEmpty()) {
            throw new FlowableException("必须包含2个连接点");
        }
        if (sourceList.size() == 1 && targetList.size() == 1) {
            // 都在第0层且不相等
            if (!sourceList.get(0).equals(targetList.get(0))) {
                return 0;
            } else {// 都在第0层且相等
                return -1;
            }
        }

        int minSize = Math.min(sourceList.size(), targetList.size());
        Integer targetLevel = null;
        for (int i = 0; i < minSize; i++) {
            if (!sourceList.get(i).equals(targetList.get(i))) {
                targetLevel = i;
                break;
            }
        }
        if (targetLevel == null) {
            if (sourceList.size() == targetList.size()) {
                targetLevel = -1;
            } else {
                targetLevel = minSize;
            }
        }
        return targetLevel;
    }

    public static Set<String> getParentExecutionIdsByActivityId(List<ExecutionEntity> executions, String activityId) {
        List<ExecutionEntity> activityIdExecutions =
                executions.stream().filter(e -> activityId.equals(e.getActivityId())).collect(Collectors.toList());
        if (activityIdExecutions.isEmpty()) {
            throw new FlowableException(activityId + " 被激活的执行实例找不到");
        }
        // check for a multi instance root execution
        ExecutionEntity miExecution = null;
        boolean isInsideMultiInstance = false;
        for (ExecutionEntity possibleMiExecution : activityIdExecutions) {
            if (possibleMiExecution.isMultiInstanceRoot()) {
                miExecution = possibleMiExecution;
                isInsideMultiInstance = true;
                break;
            }
            if (isExecutionInsideMultiInstance(possibleMiExecution)) {
                isInsideMultiInstance = true;
            }
        }
        Set<String> parentExecutionIds = new HashSet<>();
        if (isInsideMultiInstance) {
            Stream<ExecutionEntity> executionEntitiesStream = activityIdExecutions.stream();
            if (miExecution != null) {
                executionEntitiesStream = executionEntitiesStream.filter(ExecutionEntity::isMultiInstanceRoot);
            }
            executionEntitiesStream.forEach(childExecution -> {
                parentExecutionIds.add(childExecution.getParentId());
            });
        } else {
            ExecutionEntity execution = activityIdExecutions.iterator().next();
            parentExecutionIds.add(execution.getParentId());
        }
        return parentExecutionIds;
    }

    public static boolean isExecutionInsideMultiInstance(ExecutionEntity execution) {
        return getFlowElementMultiInstanceParentId(execution.getCurrentFlowElement()).isPresent();
    }

    public static Optional<String> getFlowElementMultiInstanceParentId(FlowElement flowElement) {
        FlowElementsContainer parentContainer = flowElement.getParentContainer();
        while (parentContainer instanceof Activity) {
            if (isFlowElementMultiInstance((Activity) parentContainer)) {
                return Optional.of(((Activity) parentContainer).getId());
            }
            parentContainer = ((Activity) parentContainer).getParentContainer();
        }
        return Optional.empty();
    }

    public static boolean isFlowElementMultiInstance(FlowElement flowElement) {
        if (flowElement instanceof Activity) {
            return ((Activity) flowElement).getLoopCharacteristics() != null;
        }
        return false;
    }

    public static String getParentExecutionIdFromParentIds(ExecutionEntity execution, Set<String> parentExecutionIds) {
        ExecutionEntity taskParentExecution = execution.getParent();
        String realParentExecutionId = null;
        while (taskParentExecution != null) {
            if (parentExecutionIds.contains(taskParentExecution.getId())) {
                realParentExecutionId = taskParentExecution.getId();
                break;
            }
            taskParentExecution = taskParentExecution.getParent();
        }
        if (realParentExecutionId == null || realParentExecutionId.length() == 0) {
            throw new FlowableException(execution.getId() + " 的父级执行实例找不到");
        }
        return realParentExecutionId;
    }

    public static String[] getSourceAndTargetRealActivityId(FlowNode sourceFlowElement, FlowNode targetFlowElement) {
        // 实际应操作的当前节点ID
        String sourceNodeId = sourceFlowElement.getId();
        // 实际应操作的目标节点ID
        String targetNodeId = targetFlowElement.getId();
        List<String> sourceParentProcess = FlowableUtils.getParentProcessIds(sourceFlowElement);
        List<String> targetParentProcess = FlowableUtils.getParentProcessIds(targetFlowElement);
        int diffParentLevel = getDiffLevel(sourceParentProcess, targetParentProcess);
        if (diffParentLevel != -1) {
            sourceNodeId = sourceParentProcess.size() == diffParentLevel ? sourceNodeId : sourceParentProcess.get(diffParentLevel);
            targetNodeId = targetParentProcess.size() == diffParentLevel ? targetNodeId : targetParentProcess.get(diffParentLevel);
        }
        return new String[]{sourceNodeId, targetNodeId};
    }
}

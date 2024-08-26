package com.wflow.workflow.config.listener;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.workflow.bean.dto.NotifyDto;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.bean.vo.ProcessHandlerParamsVo;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.service.NotifyService;
import com.wflow.workflow.service.ProcessNodeCacheService;
import com.wflow.workflow.service.UserDeptOrLeaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @description : 用户任务监听器
 * @author : willian fu
 * @date : 2022/8/23
 */
@Slf4j
@Component("userTaskListener")
public class UserTaskListener implements TaskListener{

    //创建一个缓存，储存审批同意的活动流程实例的状态，流程结束就释放
    //流程实例 -> (审批节点ID -> 节点内点了同意的人员)
    public static final Map<String, Map<String, Set<String>>> TASK_AGREES = new ConcurrentHashMap<>();

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserDeptOrLeaderService userService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private WflowModelHistorysMapper historysMapper;

    @Autowired
    private ProcessNodeCacheService nodeCatchService;

    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("delegateTask:{},{},{}", delegateTask.getExecutionId(),delegateTask.getEventName(),delegateTask.getAssignee());
        //获取用户任务节点ID
        String nodeId = delegateTask.getTaskDefinitionKey();
        String instanceId = delegateTask.getProcessInstanceId();
        String defId = delegateTask.getProcessDefinitionId();
        //assignment时间早于create触发，此时还没有创建成功task
        if ("create".equals(delegateTask.getEventName())) {
            //当任务被指派时，判断任务指派人是不是属于系统自动办理
            String assignee = delegateTask.getAssignee();
            boolean isAgree = WflowGlobalVarDef.WFLOW_TASK_AGRRE.equals(assignee);
            //获取下节点类型
            ProcessNode<?> node = nodeCatchService.getProcessNode(defId, nodeId);
            if (isAgree || WflowGlobalVarDef.WFLOW_TASK_REFUSE.equals(assignee)) {
                Map<String, Object> var = new HashMap<>();
                ProcessHandlerParamsVo.Action action = NodeTypeEnum.TASK.equals(node.getType()) ? ProcessHandlerParamsVo.Action.complete
                        : (isAgree ? ProcessHandlerParamsVo.Action.agree : ProcessHandlerParamsVo.Action.refuse);
                //设置处理结果
                var.put(WflowGlobalVarDef.TASK_RES_PRE + delegateTask.getId(), action);
                delegateTask.setDescription(action.toString());
                taskService.complete(delegateTask.getId(), var);
                log.info("无审批人任务节点[{}]，交付系统控制[审批结果 {}]", nodeId, isAgree);
            } else if (NodeTypeEnum.APPROVAL.equals(node.getType()) && autoSkipRepeatTask(defId, nodeId, instanceId, assignee)){
                //自动完成任务
                Authentication.setAuthenticatedUserId(delegateTask.getAssignee());
                taskService.addComment(delegateTask.getId(), instanceId, JSONObject.toJSONString(
                        new ProcessHandlerParamsVo.ProcessComment("自动处理：任务去重自动通过", Collections.emptyList())
                ));
                Authentication.setAuthenticatedUserId(null);
                delegateTask.setDescription(ProcessHandlerParamsVo.Action.agree.toString());
                taskService.complete(delegateTask.getId(), MapUtil.of(WflowGlobalVarDef.TASK_RES_PRE + delegateTask.getId(), ProcessHandlerParamsVo.Action.agree));
            } else {
                processChangeNotify(delegateTask, false);
                log.info("实例[{}]的节点[{}]任务被指派给[{}]处理", instanceId, nodeId, assignee);
            }
        } else if ("complete".equals(delegateTask.getEventName())) {
            //当任务完成时，判断是不是驳回，驳回就执行驳回操作
            ProcessHandlerParamsVo.Action action = delegateTask.getVariable(WflowGlobalVarDef.TASK_RES_PRE + delegateTask.getId(), ProcessHandlerParamsVo.Action.class);
            //完成任务的时候记录下当前节点，给下个节点使用
            if (ProcessHandlerParamsVo.Action.agree.equals(action)){
                //审批同意就进行缓存
                Map<String, Set<String>> approvalRes = getApprovalRes(instanceId);
                Set<String> agrees = approvalRes.get(nodeId);
                if (CollectionUtil.isEmpty(agrees)){
                    agrees = new LinkedHashSet<>();
                }
                agrees.add(delegateTask.getAssignee());
                approvalRes.put(nodeId, agrees);
                TASK_AGREES.put(instanceId, approvalRes);
                //说明是审批同意，记录下当前节点给后面节点相邻去重使用，注意：之前版本办理也是agree，这个更新后办理操作是complete
                runtimeService.setVariable(instanceId, WflowGlobalVarDef.PREVIOUS_AP_NODE, nodeId);
            }else {
                //其他操作则删除这个标记
                runtimeService.removeVariable(instanceId, WflowGlobalVarDef.PREVIOUS_AP_NODE);
            }
            //获取当前执行实例
            if (ProcessHandlerParamsVo.Action.refuse.equals(action)) {
                List<Execution> executions = runtimeService.createExecutionQuery().parentId(instanceId).onlyChildExecutions().list();
                Map nodeProps = delegateTask.getVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS, Map.class);
                ApprovalProps props = (ApprovalProps) nodeProps.get(nodeId);
                String target = "TO_NODE".equals(props.getRefuse().getType()) ? props.getRefuse().getTarget() : "refuse-end";
                //强制流程指向驳回/其他
                ChangeActivityStateBuilder builder = runtimeService.createChangeActivityStateBuilder().processInstanceId(instanceId);
                //这里修复并行分支内驳回分支内节点时影响其他分支的情况
                if (executions.size() > 1 && "refuse-end".equals(target)) {
                    //多实例
                    builder.moveExecutionsToSingleActivityId(executions.stream().map(Execution::getId)
                            .collect(Collectors.toList()), target).changeState();
                } else {
                    builder.moveActivityIdTo(nodeId, target).changeState();
                }
                if ("TO_END".equals(props.getRefuse().getType())) {
                    processChangeNotify(delegateTask, true);
                }
            }
            log.info("任务[{} - {}]由[{}]完成", nodeId, delegateTask.getName(), delegateTask.getAssignee());
        }
    }

    private void processChangeNotify(DelegateTask delegateTask, boolean isRefuse) {
        String instanceId = delegateTask.getProcessInstanceId();
        String dfId = delegateTask.getProcessDefinitionId();
        String startUser = String.valueOf(delegateTask.getVariable(WflowGlobalVarDef.INITIATOR));
        String assignee = delegateTask.getAssignee();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult();
        String defName = "??";
        if (Objects.nonNull(processInstance)) {
            defName = processInstance.getProcessDefinitionName();
        } else {
            WflowModelHistorys modelHistory = historysMapper.selectOne(new LambdaQueryWrapper<WflowModelHistorys>()
                    .select(WflowModelHistorys::getFormName).eq(WflowModelHistorys::getProcessDefId, dfId));
            if (Objects.nonNull(modelHistory)) {
                defName = modelHistory.getFormName();
            }
        }
        OrgUser orgUser = userService.getUserMapByIds(CollectionUtil.newArrayList(startUser)).get(startUser);
        notifyService.notify(NotifyDto.builder()
                .title(isRefuse ? "您提交的审批被驳回" : "您有新的待办任务")
                .processDefId(dfId)
                .instanceId(instanceId)
                .nodeId(delegateTask.getTaskDefinitionKey())
                .target(isRefuse ? startUser : assignee)
                .content(isRefuse ? StrUtil.builder("您提交的【", defName, "】已被驳回").toString()
                        : StrUtil.builder(orgUser.getName(), " 提交的【", defName, "】需要您审批，请即时处理"
                ).toString())
                .type(isRefuse ? NotifyDto.TypeEnum.ERROR : NotifyDto.TypeEnum.WARNING)
                .build());
    }

    private boolean autoSkipRepeatTask(String defId, String nodeId, String instanceId, String assignee){
        //取流程设置项
        WflowModelHistorys model = historysMapper.selectOne(new LambdaQueryWrapper<WflowModelHistorys>()
                .select(WflowModelHistorys::getSettings).eq(WflowModelHistorys::getProcessDefId, defId));
        if (Objects.isNull(model)){
            return false;
        }
        JSONObject settings = JSONObject.parseObject(model.getSettings());
        //取去重设置
        String sameSkip = settings.getString("sameSkip");
        if (StrUtil.isBlank(sameSkip)) {
            return false;
        }
        Map<String, Set<String>> approvalRes = getApprovalRes(instanceId);
        boolean haReturn = runtimeService.hasVariable(instanceId, WflowGlobalVarDef.NODE_RETURN);
        switch (sameSkip){
            case "NONE": return false;
            case "FIRST": //仅第一个审批节点需要审批
                if (haReturn && Boolean.FALSE.equals(settings.getBoolean("recallSkip"))){
                    //如果有回退且不自动同意
                    return false;
                }
                return approvalRes.entrySet().stream().anyMatch(v -> v.getValue().contains(assignee));
            case "NEXT": //取相邻节点审批记录，如果中间隔了办理节点就不算相邻，还有一种情况是，在并行分支下面，那么就要遍历所有并行分支下面的节点了
                //获取上一个节点ID
                if (haReturn) {
                    return false;
                }
                String beforeNode = runtimeService.getVariable(instanceId, WflowGlobalVarDef.PREVIOUS_AP_NODE, String.class);
                if (StrUtil.isNotBlank(beforeNode)){
                    Set<String> agrees = approvalRes.get(beforeNode);
                    return Objects.nonNull(agrees) && agrees.contains(assignee);
                }
        }
        return false;
    }

    /**
     * 获取流程同意的审批记录
     * @param instanceId 实例ID
     * @return 审批记录Map<节点ID, 审批人ID>
     */
    private Map<String, Set<String>> getApprovalRes(String instanceId){
        Map<String, Set<String>> taskResults = TASK_AGREES.get(instanceId);
        if (CollectionUtil.isEmpty(taskResults)){
            //加载该流程所有的审批同意的记录
            List<HistoricTaskInstance> hisTasks = historyService.createNativeHistoricTaskInstanceQuery()
                    .sql(" SELECT ht.* FROM ACT_HI_TASKINST ht INNER JOIN ACT_HI_VARINST hv ON hv.NAME_ = CONCAT('approve_', ht.ID_) AND hv.TEXT_ = 'agree' " +
                            "WHERE ht.PROC_INST_ID_ = #{instanceId} AND ht.END_TIME_ IS NOT NULL ORDER BY ht.START_TIME_ ASC")
                    .parameter("instanceId", instanceId).list();
            //将记录进行缓存，分组聚合
            taskResults = hisTasks.stream().collect(Collectors.groupingBy(
                    HistoricTaskInstance::getTaskDefinitionKey,
                    LinkedHashMap::new,
                    Collectors.mapping(HistoricTaskInstance::getAssignee, Collectors.toCollection(LinkedHashSet::new))
            ));
            if (CollectionUtil.isNotEmpty(taskResults)){
                TASK_AGREES.put(instanceId, taskResults);
            }else {
                return new ConcurrentHashMap<>();
            }
        }
        return taskResults;
    }
}

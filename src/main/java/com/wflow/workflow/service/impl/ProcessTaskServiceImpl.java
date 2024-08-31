package com.wflow.workflow.service.impl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.entity.WflowUserAgents;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.mapper.WflowModelsMapper;
import com.wflow.mapper.WflowUserAgentsMapper;
import com.wflow.service.OrgRepositoryService;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.dto.NotifyDto;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.enums.ApprovalModeEnum;
import com.wflow.workflow.bean.process.enums.ApprovalTypeEnum;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.bean.process.props.CcProps;
import com.wflow.workflow.bean.vo.*;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.extension.cmd.RecallToHisApprovalNodeCmd;
import com.wflow.workflow.service.*;
import com.wflow.workflow.utils.Executor;
import com.wflow.workflow.utils.FlowableUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2022/8/25
 */
@Slf4j
@Service("processTaskService")
public class ProcessTaskServiceImpl implements ProcessTaskService {

    private final static OrgUser UNKNOW_USER = OrgUser.builder().id("5201314").name("人员待定").build();

    @Autowired
    private TaskService taskService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private ProcessStepRenderService stepRenderService;

    @Autowired
    private BusinessDataStorageService businessDataService;

    @Autowired
    private UserDeptOrLeaderService userDeptOrLeaderService;

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private WflowModelHistorysMapper historysMapper;

    @Autowired
    private ProcessNodeCacheService nodeCatchService;

    @Autowired
    private WflowUserAgentsMapper agentsMapper;

    //超时缓存，数据缓存20秒，用来存储审批人防止flowable高频调用 //TODO 缓存优化
    private static final TimedCache<String, List<String>> taskCache = CacheUtil.newTimedCache(20000);

    //用来存储正在处理的节点，防止并发处理
    private static final Set<String> HANDLER_NODE_LOCK = new ConcurrentHashSet<>();

    static {
        taskCache.schedulePrune(10000);
    }

    @Override
    public Page<ProcessTaskVo> getUserTodoList(Integer pageSize, Integer pageNo, String code,
                                               String[] startTimes, String keyword) {
        String userId = UserUtil.getLoginUserId();
        TaskQuery taskQuery = taskService.createTaskQuery();
        Executor.builder().ifNotBlankNext(code, taskQuery::processDefinitionKey)
                .ifTrueNext(null != startTimes && startTimes.length > 1, () -> {
                    taskQuery.taskCreatedAfter(DateUtil.parse(startTimes[0]));
                    taskQuery.taskCreatedBefore(DateUtil.parse(startTimes[1]));
                })
                .ifNotBlankNext(keyword, v -> {
                    taskQuery.or().taskNameLike("%" + keyword.trim() + "%")
                            .processDefinitionNameLike("%" + keyword.trim() + "%")
                            .endOr();
                });
        taskQuery.active().taskCandidateOrAssigned(userId).orderByTaskCreateTime().desc();
        Page<ProcessTaskVo> page = new Page<>();
        List<Task> taskList = taskQuery.listPage(pageSize * (pageNo - 1), pageSize);
        page.setTotal(taskQuery.count());
        page.setCurrent(pageNo);
        page.setSize(pageSize);
        page.setRecords(Collections.emptyList());
        Set<String> staterUsers = new HashSet<>();
        Map<String, List<FormAbstractsVo>> abstractDatas = businessDataService.getInstanceAbstractDatas(taskList.stream()
                .collect(Collectors.toMap(Task::getProcessInstanceId, Task::getProcessDefinitionId, (v1, v2) -> v1)));
        Set<String> instanceIds = taskList.stream().map(Task::getProcessInstanceId).collect(Collectors.toSet());
        Map<String, Object> startDept = FlowableUtils.getProcessVars(instanceIds, WflowGlobalVarDef.START_DEPT);
        //把待办任务流程实例一次性取出来，减少查询次数
        Map<String, ProcessInstance> instanceMap = CollectionUtil.isNotEmpty(taskList) ?
                runtimeService.createProcessInstanceQuery().processInstanceIds(taskList.stream()
                                .map(Task::getProcessInstanceId).collect(Collectors.toSet()))
                        .list().stream().collect(Collectors.toMap(ProcessInstance::getId, v -> v)) : new HashMap<>();
        List<ProcessTaskVo> taskVos = taskList.stream().map(task -> {
            ProcessInstance instance = instanceMap.get(task.getProcessInstanceId());
            //构造用户id -> 部门id
            String deptId = String.valueOf(Optional.ofNullable(startDept.get(task.getProcessInstanceId()))
                    //如果没有就从流程变量 owner 里取（之前是存owner变量）
                    .orElseGet(() -> FlowableUtils.getOwnerDept(task.getProcessInstanceId(), true)));
            staterUsers.add(instance.getStartUserId() + "_" + deptId);
            return ProcessTaskVo.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .taskDefKey(task.getTaskDefinitionKey())
                    .processDefId(task.getProcessDefinitionId())
                    .executionId(task.getExecutionId())
                    .nodeId(task.getTaskDefinitionKey())
                    .deployId(instance.getDeploymentId())
                    .processDefName(instance.getProcessDefinitionName())
                    .version(instance.getProcessDefinitionVersion())
                    .instanceId(task.getProcessInstanceId())
                    .superInstanceId(instance.getRootProcessInstanceId())
                    .ownerId(instance.getStartUserId())
                    .ownerDeptId(deptId)
                    .createTime(instance.getStartTime())
                    .taskCreateTime(task.getCreateTime())
                    .formAbstracts(abstractDatas.getOrDefault(task.getProcessInstanceId(), Collections.emptyList()))
                    .build();
        }).collect(Collectors.toList());
        //取用户信息，减少数据库查询，一次构建
        if (CollectionUtil.isNotEmpty(staterUsers)) {
            Map<String, UserDeptDo> infoMap = orgRepositoryService.getUserDeptInfos(staterUsers);
            taskVos.forEach(v -> {
                UserDeptDo userDeptDo = infoMap.get(v.getOwnerId() + "_" + v.getOwnerDeptId());
                if (Objects.nonNull(userDeptDo)) {
                    v.setOwner(OrgUser.builder()
                            .name(userDeptDo.getUserName())
                            .avatar(userDeptDo.getAvatar())
                            .id(userDeptDo.getUserId())
                            .build());
                    v.setOwnerDeptId(userDeptDo.getDeptId());
                    v.setOwnerDeptName(userDeptDo.getDeptName());
                }
            });
            page.setRecords(taskVos);
        }
        return page;
    }

    @Override
    public Page<ProcessTaskVo> getUserIdoList(Integer pageSize, Integer pageNo, String code) {
        String userId = UserUtil.getLoginUserId();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();
        Executor.builder().ifNotBlankNext(code, query::processDefinitionKey);
        query.taskAssignee(userId).finished().taskDefinitionKeyLike("node_%").orderByHistoricTaskInstanceEndTime().desc();
        List<HistoricTaskInstance> taskInstances = query.listPage(pageSize * (pageNo - 1), pageSize);
        //把已办任务流程实例一次性取出来，减少查询次数
        Map<String, HistoricProcessInstance> instanceMap = CollectionUtil.isNotEmpty(taskInstances) ?
                historyService.createHistoricProcessInstanceQuery().processInstanceIds(taskInstances.stream()
                                .map(HistoricTaskInstance::getProcessInstanceId).collect(Collectors.toSet()))
                        .list().stream().collect(Collectors.toMap(HistoricProcessInstance::getId, v -> v)) : new HashMap<>();
        Page<ProcessTaskVo> page = new Page<>();
        page.setTotal(query.count());
        page.setCurrent(pageNo);
        page.setSize(pageSize);
        Set<String> staterUsers = new HashSet<>();
        Map<String, List<FormAbstractsVo>> abstractDatas = businessDataService.getInstanceAbstractDatas(instanceMap.values().stream()
                .collect(Collectors.toMap(HistoricProcessInstance::getId, HistoricProcessInstance::getProcessDefinitionId, (v1, v2) -> v1)));
        //批量获取所有任务的发起部门信息
        Map<String, Object> startDept = FlowableUtils.getProcessVars(instanceMap.keySet(), WflowGlobalVarDef.START_DEPT);
        Map<String, Object> taskResults = FlowableUtils.getProcessVars(instanceMap.keySet(),
                taskInstances.stream().map(task -> WflowGlobalVarDef.TASK_RES_PRE + task.getId()).collect(Collectors.toList()));
        List<ProcessTaskVo> taskVos = taskInstances.stream().map(task -> {
            HistoricProcessInstance instance = instanceMap.get(task.getProcessInstanceId());
            Object taskResult = taskResults.get(task.getProcessInstanceId() + WflowGlobalVarDef.TASK_RES_PRE + task.getId());
            //构造用户id -> 部门id
            String deptId = String.valueOf(Optional.ofNullable(startDept.get(task.getProcessInstanceId()))
                    //如果没有就从流程变量 owner 里取（之前是存owner变量）
                    .orElseGet(() -> FlowableUtils.getOwnerDept(task.getProcessInstanceId(), false)));
            staterUsers.add(instance.getStartUserId() + "_" + deptId);
            return ProcessTaskVo.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .taskDefKey(task.getTaskDefinitionKey())
                    .processDefId(task.getProcessDefinitionId())
                    .executionId(task.getExecutionId())
                    .nodeId(task.getTaskDefinitionKey())
                    .deployId(instance.getDeploymentId())
                    .superInstanceId(Optional.ofNullable(instance.getSuperProcessInstanceId()).orElse(instance.getId()))
                    .processDefName(instance.getProcessDefinitionName())
                    .version(instance.getProcessDefinitionVersion())
                    .instanceId(task.getProcessInstanceId())
                    .ownerId(instance.getStartUserId())
                    .ownerDeptId(deptId)
                    .createTime(instance.getStartTime())
                    .taskCreateTime(task.getCreateTime())
                    .taskEndTime(task.getEndTime())
                    .taskResult(Objects.nonNull(taskResult) ? String.valueOf(taskResult) : null)
                    .formAbstracts(abstractDatas.getOrDefault(task.getProcessInstanceId(), Collections.emptyList()))
                    .build();
        }).collect(Collectors.toList());
        //取用户及部门信息，减少数据库查询，一次构建
        if (CollectionUtil.isNotEmpty(staterUsers)) {
            Map<String, UserDeptDo> infoMap = orgRepositoryService.getUserDeptInfos(staterUsers);
            taskVos.forEach(v -> {
                UserDeptDo userDeptDo = infoMap.get(v.getOwnerId() + "_" + v.getOwnerDeptId());
                if (Objects.nonNull(userDeptDo)) {
                    v.setOwner(OrgUser.builder()
                            .name(userDeptDo.getUserName())
                            .avatar(userDeptDo.getAvatar())
                            .id(userDeptDo.getUserId())
                            .build());
                    v.setOwnerDeptId(userDeptDo.getDeptId());
                    v.setOwnerDeptName(userDeptDo.getDeptName());
                }
            });
            page.setRecords(taskVos);
        }
        return page;
    }

    @Override
    @Transactional
    public void approvalTask(String taskUser, String operationUser, ProcessHandlerParamsVo params) {
        Authentication.setAuthenticatedUserId(taskUser);
        Task task = null;
        if (StrUtil.isNotBlank(params.getTaskId())) {
            task = taskService.createTaskQuery().taskId(params.getTaskId()).active().singleResult();
        }
        boolean isComment = ProcessHandlerParamsVo.Action.comment.equals(params.getAction());
        boolean isCancel = ProcessHandlerParamsVo.Action.cancel.equals(params.getAction());
        boolean isComplete = ProcessHandlerParamsVo.Action.complete.equals(params.getAction());
        boolean isAgree = ProcessHandlerParamsVo.Action.agree.equals(params.getAction());
        if (hasComment(params.getComment())) {
            //有评论内容，且是撤销操作，在评论内容前默认加上撤销字样
            if (isCancel) {
                params.getComment().setText("撤销：" + params.getComment().getText());
            }
            if (!taskUser.equals(operationUser)) {
                //流程被干预添加痕迹
                UserDo operationU = orgRepositoryService.getUserById(operationUser);
                params.getComment().setText(StrUtil.builder("[")
                        .append(operationU.getUserName()).append("] 干预流程：")
                        .append(params.getComment().getText()).toString());
            }
            taskService.addComment(params.getTaskId(), params.getInstanceId(), JSONObject.toJSONString(params.getComment()));
        }
        if (isComment) return;
        //审批同意有带签名就更新签名
        if (StrUtil.isNotBlank(params.getSignature())) {
            if (isAgree && params.getUpdateSign()) {
                orgRepositoryService.updateUserSign(operationUser, params.getSignature());
            }
        }
        if (isCancel) {
            //撤销操作
            List<ActivityInstance> instances = runtimeService.createActivityInstanceQuery()
                    .processInstanceId(params.getInstanceId()).unfinished().list();
            doCancelProcess(params.getInstanceId(),
                    instances.stream()
                            .filter(v -> v.getActivityType().equalsIgnoreCase("userTask"))
                            .findFirst().get().getActivityId());
        } else {
            //处理非评论及撤销逻辑
            if (ObjectUtil.isNull(task)) {
                throw new BusinessException("任务已结束，请刷新数据再试");
            } else if ("root".equals(task.getTaskDefinitionKey()) && !isComplete) {
                //发起人节点只能撤销或同意提交
                throw new BusinessException("发起人节点不支持此项操作");
            } else {
                String lockKey = task.getProcessInstanceId() + task.getTaskDefinitionKey();
                //添加锁，防止并发处理
                if (HANDLER_NODE_LOCK.contains(lockKey)) {
                    throw new BusinessException("当前节点正在处理中，请稍后再试");
                }
                HANDLER_NODE_LOCK.add(lockKey);
                try {
                    task.setDescription(params.getAction().toString());
                    switch (params.getAction()) {
                        case agree:
                        case refuse:
                        case complete:
                            businessDataService.updateInstanceFormData(operationUser, params.getInstanceId(), params.getFormData());
                            doApproval(task, params);
                            break;
                        case recall: //退回
                            doRecallTask(task, operationUser, params);
                            break;
                        case transfer: //转交
                            businessDataService.updateInstanceFormData(operationUser, params.getInstanceId(), params.getFormData());
                            doTransferTask(task, params, operationUser);
                            break;
                        case afterAdd:
                        case beforeAdd: //加签，暂时只支持后加签
                            //加签如果是顺序会签，暂时先禁止，
                            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
                            UserTask userTask = (UserTask) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());
                            MultiInstanceLoopCharacteristics loopCharacteristics = userTask.getLoopCharacteristics();
                            if (Objects.isNull(loopCharacteristics) || loopCharacteristics.isSequential()) {
                                throw new BusinessException("当前节点不支持临时加签");
                            }
                            runtimeService.addMultiInstanceExecution(task.getTaskDefinitionKey(),
                                    task.getProcessInstanceId(), Collections.singletonMap("assignee", params.getTargetUser()));
                            log.info("[{}]将任务[{}]加签给[{}]处理", operationUser, task.getId(), params.getTargetUser());
                            break;
                    }
                } catch (Exception e) {
                    log.error("处理任务异常", e);
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                } finally {
                    //移除锁
                    HANDLER_NODE_LOCK.remove(lockKey);
                }
            }
            Authentication.setAuthenticatedUserId(null);
        }
    }

    @Override
    public Set<String> getCcTaskUsers(String instanceId, String nodeId) {
        //获取设置项
        HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instanceId)
                .variableName(WflowGlobalVarDef.WFLOW_NODE_PROPS).singleResult();
        Map nodeProps;
        if (Objects.nonNull(variableInstance)) {
            nodeProps = (Map) variableInstance.getValue();
        } else {
            //流程首个节点需要从执行实例中取数据
            nodeProps = runtimeService.getVariable(instanceId, WflowGlobalVarDef.WFLOW_NODE_PROPS, Map.class);
        }
        CcProps ccProps = (CcProps) nodeProps.get(nodeId);
        //获取变量里面自选的抄送人
        Set<String> ccUsers = new HashSet<>();
        List<OrgUser> orgs = new ArrayList<>(ccProps.getAssignedUser());
        if (ccProps.getShouldAdd()) {
            //获取发起流程时添加的抄送人
            HistoricVariableInstance result = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(instanceId)
                    .variableName(nodeId).singleResult();
            if (Objects.nonNull(result)) {
                Optional.ofNullable(result.getValue()).ifPresent(us -> orgs.addAll((List<OrgUser>) us));
            } else {
                Optional.ofNullable(runtimeService.getVariable(instanceId, nodeId, List.class)).ifPresent(orgs::addAll);
            }
        }
        //解析部门与人员选项
        ccUsers.addAll(orgs.stream().filter(org -> "user".equals(org.getType()))
                .map(OrgUser::getId).collect(Collectors.toSet()));
        ccUsers.addAll(userDeptOrLeaderService.getUsersByDept(orgs.stream()
                .filter(org -> "dept".equals(org.getType()))
                .map(OrgUser::getId).collect(Collectors.toSet())));
        return ccUsers;
    }

//    TODO spring EL 表达式调用
    @Override
    public List<String> getNodeApprovalUsers(ExecutionEntity execution) {
        //取缓存里面的，判断之前有没有，多实例防止多次解析
        List<String> cacheUsers = taskCache.get(execution.getProcessInstanceId() + execution.getActivityId());
        if (Objects.nonNull(cacheUsers)) {
            return cacheUsers;
        }
        log.info("获取节点[{}]的审批人", execution.getActivityId());
        Map propsMap = execution.getVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS, Map.class);
        ApprovalProps props = (ApprovalProps) propsMap.get(execution.getActivityId());
        List<String> approvalUsers = getApprovalUsers(execution.getProcessInstanceId(), execution.getActivityId(), props);
        taskCache.put(execution.getProcessInstanceId() + execution.getActivityId(), approvalUsers);
        return approvalUsers;
    }

    @Override
    public List<ProcessProgressVo.ProgressNode> getFutureTask(HistoricProcessInstance instance, String
            startDept, Map<String, Object> context, Map<String, ProcessNode<?>> nodes) {
        //根据流程遍历后续节点，期间要穿越后续包含并行网关和条件网关的节点，先找到所有激活的任务节点开始递归
        //节点如果处于并行/包容分支内，可能会有多个活动的节点
        List<ProcessProgressVo.ProgressNode> progressNodes = new LinkedList<>();
        try {
            Set<String> idSet = new LinkedHashSet<>();
            context.put("root", instance.getStartUserId());
            taskService.createTaskQuery().processInstanceId(instance.getId()).active().list()
                    .stream().map(TaskInfo::getTaskDefinitionKey)
                    .collect(Collectors.toSet()).forEach(nodeId -> {
                        //根据每个活动的节点进行遍历，最终它们会在合流点汇聚
                        idSet.add(nodeId);
                        Optional.ofNullable(nodes.get(nodeId)).ifPresent(node -> {
                            //已激活的当前节点，需要进行去重
                            if (node.getProps() instanceof ApprovalProps) {
                                ApprovalProps props = (ApprovalProps) node.getProps();
                                List<String> users = getApprovalUsers(instance.getId(), node.getId(), props);
                                //取已下发的任务
                                List<HistoricTaskInstance> ingTask = historyService.createHistoricTaskInstanceQuery()
                                        .processInstanceId(instance.getId()).taskDefinitionKey(nodeId).list();
                                Set<String> ingTaskUser = ingTask.stream().map(TaskInfo::getAssignee).collect(Collectors.toSet());
                                Map<String, OrgUser> userMaps = userDeptOrLeaderService.getUserMapByIds(users);
                                users.stream().filter(v -> !ingTaskUser.contains(v))
                                        .forEach(us -> {
                                            Date createTime = ingTask.get(ingTask.size() - 1).getCreateTime();
                                            //把时间往后延长10ms，使其排列到后方
                                            createTime.setTime(createTime.getTime() + 10);
                                            progressNodes.add(ProcessProgressVo.ProgressNode.builder()
                                                    .nodeId(node.getId())
                                                    .isFuture(true)
                                                    .name(node.getName())
                                                    .user(userMaps.getOrDefault(us, UNKNOW_USER))
                                                    .nodeType(node.getType())
                                                    .comment(Collections.emptyList())
                                                    .approvalMode(props.getMode())
                                                    .startTime(createTime)
                                                    .build());
                                        });
                            }
                            //遍历后续节点
                            foreachNode( node.getChildren(), idSet, progressNodes, instance, context);
                        });
                    });
            //继续搜寻剩下的节点，直到结束，处理
            if (CollectionUtil.isNotEmpty(idSet)){
                foreachGatewayNext(idSet, nodes, progressNodes, instance, context);
            }
        } catch (Exception e) {
            log.error("获取[{}]未开始任务异常: {}", instance.getId(), e.getMessage());
        }
        return progressNodes;
    }

    private void foreachGatewayNext(Set<String> idSet, Map<String, ProcessNode<?>> nodes,
                                    List<ProcessProgressVo.ProgressNode> progressNodes,
                                    HistoricProcessInstance instance, Map<String, Object> vars){
        List<String> list = new ArrayList<>(idSet);
        ProcessNode<?> lastNode = nodes.get(list.get(list.size() - 1));
        if (StrUtil.isNotBlank(lastNode.getGatewayId())){
            //拿到网关ID，继续往下遍历
            ProcessNode<?> gateway = nodes.get(lastNode.getGatewayId());
            //找到空节点后续节点
            ProcessNode<?> emptyNext = gateway.getChildren().getChildren();
            idSet.add(gateway.getChildren().getId());
            if (Objects.nonNull(emptyNext) && StrUtil.isNotBlank(emptyNext.getId())){
                foreachNode(emptyNext, idSet, progressNodes, instance, vars);
            }
            foreachGatewayNext(idSet, nodes, progressNodes, instance, vars);
        }
    }

    private void foreachNode(ProcessNode<?> node, Set<String> idSet, List<ProcessProgressVo.ProgressNode> progressNodes,
                             HistoricProcessInstance instance, Map<String, Object> vars) {
        if (Objects.isNull(node) || StrUtil.isBlank(node.getId()) || idSet.contains(node.getId())) return;
        idSet.add(node.getId());
        if ((NodeTypeEnum.TASK.equals(node.getType()) || NodeTypeEnum.APPROVAL.equals(node.getType())
                || NodeTypeEnum.CC.equals(node.getType()))) {
            //没有遍历过的节点，且是（审批、抄送、办理）把对应的人员解析出来
            Collection<String> users;
            ApprovalModeEnum modeEnum;
            if (NodeTypeEnum.CC.equals(node.getType())) {
                modeEnum = null;
                users = getCcTaskUsers(instance.getId(), node.getId());
            } else {
                ApprovalProps props = (ApprovalProps) node.getProps();
                modeEnum = props.getMode();
                users = getApprovalUsers(instance.getId(), node.getId(), props);
            }
            Map<String, OrgUser> userMaps = userDeptOrLeaderService.getUserMapByIds(users);
            users.forEach(us -> progressNodes.add(ProcessProgressVo.ProgressNode.builder()
                    .nodeId(node.getId())
                    .isFuture(true)
                    .name(node.getName())
                    .user(userMaps.getOrDefault(us, UNKNOW_USER))
                    .nodeType(node.getType())
                    .comment(Collections.emptyList())
                    .approvalMode(modeEnum)
                    .startTime(GregorianCalendar.getInstance().getTime())
                    .build()));
        } else if (NodeTypeEnum.CONCURRENTS.equals(node.getType())) {
            //并行网关，全部需要递归遍历
            node.getBranchs().forEach(branch -> {
                foreachNode(branch, idSet, progressNodes, instance, vars);
            });
            //从分支出来继续往下遍历
        } else if (NodeTypeEnum.INCLUSIVES.equals(node.getType()) || NodeTypeEnum.CONDITIONS.equals(node.getType())) {
            //包容网关/条件网关，满足条件的分支都要执行
            List<String> trueConditions = stepRenderService.getIsTrueConditions(ProcessConditionResolveParamsVo.builder()
                    .processDfId(instance.getProcessDefinitionId())
                    .conditionNodeId(node.getId()).context(vars)
                    .multiple(NodeTypeEnum.INCLUSIVES.equals(node.getType()))
                    .build());
            //遍历满足条件的分支
            node.getBranchs().forEach(bNode -> {
                if (trueConditions.contains(bNode.getId())) {
                    foreachNode(bNode, idSet, progressNodes, instance, vars);
                }
            });
        } else if (NodeTypeEnum.EMPTY.equals(node.getType())) {
            //空节点的话是合流点，说明前面有网关，那么要回头再向上去找节点，太复杂了，难搞
            //nodes.get(node.getParentId())
        }
        foreachNode(node.getChildren(), idSet, progressNodes, instance, vars);
    }

    @Override
    public List<HisApprovalNodeVo> getRecallTaskNodes(String instanceId, String taskId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        //获取当前用户的所有待审批任务
        String userId = UserUtil.getLoginUserId();
        Task task = taskService.createTaskQuery().processInstanceId(instanceId).taskCandidateOrAssigned(userId).taskId(taskId).active().singleResult();
        if (Objects.isNull(task)) {
            throw new BusinessException("该任务不存在");
        }
        //获取已经处理过的任务
        List<ActivityInstance> activityInstances = runtimeService.createActivityInstanceQuery()
                .processInstanceId(instanceId).finished().orderByActivityInstanceStartTime().asc().list();
        Map<String, String> collect = activityInstances.stream()
                .filter(act -> BpmnXMLConstants.ELEMENT_TASK_USER.equals(act.getActivityType()))
                .collect(Collectors.toMap(ActivityInstance::getActivityId, ActivityInstance::getActivityName, (v1, v2) -> v2));
        //移除当前的自身节点
        collect.remove(task.getTaskDefinitionKey());
        if (CollectionUtil.isNotEmpty(collect)) {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
            //获取当前节点
            FlowNode nowNode = (FlowNode) bpmnModel.getFlowElement(task.getTaskDefinitionKey());
            Set<String> hisNodes = new HashSet<>();
            loadBeforeSerialUserTaskNode(nowNode, hisNodes, 0);
            //取交集
            Collection<String> intersection = new HashSet<>(CollectionUtil.intersection(collect.keySet(), hisNodes));
            //获取任务所在审批节点
            return collect.keySet().stream().filter(key -> {
                        FlowNode sflowNode = (FlowNode) bpmnModel.getFlowElement(key);
                        FlowNode tflowNode = (FlowNode) bpmnModel.getFlowElement(task.getTaskDefinitionKey());
                        return ExecutionGraphUtil.isReachable(bpmnModel.getMainProcess(), sflowNode, tflowNode, new HashSet<>())
                                && intersection.contains(key);
                    }).map(key -> new HisApprovalNodeVo(key, collect.getOrDefault(key, "--")))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public NodeSettingsVo getNodeTaskSettings(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (Objects.isNull(task)) {
            throw new BusinessException("该任务已被处理，请刷新数据");
        }
        WflowModelHistorys modelHistory = historysMapper.selectOne(
                new LambdaQueryWrapper<WflowModelHistorys>()
                        .eq(WflowModelHistorys::getProcessDefId, task.getProcessDefinitionId()));
        NodeSettingsVo settingsVo = NodeSettingsVo.builder()
                .chooseUsers(new ArrayList<>())
                .enableSign(false).build();
        if (Objects.nonNull(modelHistory)) { //子流程的话，这里是null
            Map<String, ProcessNode<?>> nodeMap = nodeCatchService.getProcessNode(task.getProcessDefinitionId());
            JSONObject object = JSONObject.parseObject(modelHistory.getSettings());
            ProcessNode<?> node = nodeMap.get(task.getTaskDefinitionKey());
            Boolean sign = object.getBoolean("sign");
            //先查全局设置，如果没开启签名，就再查流程节点设置
            if (Boolean.TRUE.equals(sign)) {
                settingsVo.setEnableSign(true);
            } else {
                if (node.getProps() instanceof ApprovalProps) {
                    ApprovalProps nodeProps = (ApprovalProps) node.getProps();
                    settingsVo.setEnableSign(nodeProps.isSign());
                }
            }
            //检查是否要指定其他节点审批人
            nodeMap.forEach((nodeId, processNode) -> {
                if (processNode.getProps() instanceof ApprovalProps) {
                    ApprovalProps props = (ApprovalProps) processNode.getProps();
                    if (ApprovalTypeEnum.OTHER_SELECT.equals(props.getAssignedType())
                            && CollectionUtil.isNotEmpty(props.getAssignedNode())
                            && props.getAssignedNode().contains(task.getTaskDefinitionKey())) {
                        List<String> users = runtimeService.getVariable(task.getProcessInstanceId(), task.getTaskDefinitionKey(), List.class);
                        List<OrgUser> orgUsers = new ArrayList<>();
                        if (CollectionUtil.isNotEmpty(users)) {
                            orgUsers.addAll(userDeptOrLeaderService.getUserMapByIds(users).values());
                        }
                        //如果当前节点在指定节点里面，就加入
                        settingsVo.getChooseUsers().addAll(props.getAssignedNode().stream().map(n ->
                                new NodeSettingsVo.ApChooseUser(processNode.getId(), processNode.getName(), orgUsers)
                        ).collect(Collectors.toList()));
                    }
                }
            });
        }
        return settingsVo;
    }

    @Override
    @Transactional
    public void workHandover(String sourceUser, String targetUser) {
        //工作交接的话，先把在途的所有流程全部执行转交，然后设置一个永久期限的审批代理人
        taskService.createTaskQuery().taskAssignee(sourceUser).active().list().forEach(task -> {
            /*task.setOwner(sourceUser);
            task.setAssignee(targetUser);*/
            taskService.setOwner(task.getId(), sourceUser);
            taskService.setAssignee(task.getId(), targetUser);
        });
        //如果A设置B为代理人，C又被A代理，那么需要更新C被B代理
        agentsMapper.update(WflowUserAgents.builder().agentUserId(targetUser).build(),
                new LambdaQueryWrapper<WflowUserAgents>().eq(WflowUserAgents::getAgentUserId, sourceUser));
        agentsMapper.delete(new LambdaQueryWrapper<WflowUserAgents>().eq(WflowUserAgents::getUserId, sourceUser));
        agentsMapper.insert(WflowUserAgents.builder().userId(sourceUser).agentUserId(targetUser)
                .startTime(new Date()).endTime(DateUtil.parseDate("2099-01-01")).build());
    }

    /**
     * 获取审批人
     *
     * @param instanceId 实例ID
     * @param nodeId     节点ID
     * @param props      节点熟悉
     * @return 审批人ID列表
     */
    public List<String> getApprovalUsers(String instanceId, String nodeId, ApprovalProps props) {
        String userId = runtimeService.getVariable(instanceId, WflowGlobalVarDef.INITIATOR, String.class);
        String deptId = runtimeService.getVariable(instanceId, WflowGlobalVarDef.START_DEPT, String.class);
        deptId = StrUtil.isNotBlank(deptId) ? deptId : runtimeService.getVariable(instanceId, WflowGlobalVarDef.OWNER, ProcessInstanceOwnerDto.class).getOwnerDeptId();
        Set<String> userSet = new LinkedHashSet<>();
        switch (props.getAssignedType()) {
            case REFUSE:
                userSet.add(WflowGlobalVarDef.WFLOW_TASK_REFUSE);
                break;
            case SELF: //取流程发起人
                userSet.add(userId);
                break;
            case ROLE: //取角色
                userSet.addAll(userDeptOrLeaderService.getUsersByRoles(props.getRole().stream().map(OrgUser::getId).collect(Collectors.toList())));
                break;
            case FORM_USER: //从表单取
                List<Map<String, Object>> userList = runtimeService.getVariable(instanceId, props.getFormUser(), List.class);
                Optional.ofNullable(userList).ifPresent(users -> {
                    userSet.addAll(users.stream().map(u -> u.get("id").toString()).collect(Collectors.toList()));
                });
                break;
            case FORM_DEPT: //从表单取
                List<Map<String, Object>> deptList = runtimeService.getVariable(instanceId, props.getFormDept(), List.class);
                Optional.ofNullable(deptList).ifPresent(users -> {
                    List<String> deptIds = users.stream().map(u -> u.get("id").toString()).collect(Collectors.toList());
                    //取设置项
                    if (Objects.nonNull(props.getDeptProp())) {
                        userSet.addAll(getDeptUsers(props.getDeptProp(), deptIds));
                    } else {
                        userSet.addAll(userDeptOrLeaderService.getLeadersByDept(deptIds));
                    }
                });
                break;
            case ASSIGN_USER://指定用户
                userSet.addAll(props.getAssignedUser().stream().map(OrgUser::getId).collect(Collectors.toList()));
                break;
            case ASSIGN_LEADER://指定部门
                List<String> collect = props.getAssignedDept().stream().map(OrgUser::getId).collect(Collectors.toList());
                //取设置项
                if (Objects.nonNull(props.getDeptProp())) {
                    userSet.addAll(getDeptUsers(props.getDeptProp(), collect));
                } else {
                    userSet.addAll(userDeptOrLeaderService.getLeadersByDept(collect));
                }
                break;
            case SELF_SELECT: //自选用户，从变量取，这一步在发起流程时设置的
                List<OrgUser> selectUsers = runtimeService.getVariable(instanceId, nodeId, List.class);
                Optional.ofNullable(selectUsers).ifPresent(on -> userSet.addAll(on.stream().map(OrgUser::getId).collect(Collectors.toList())));
                break;
            case LEADER: //用户的指定级别部门主管
                String leaderByLevel = userDeptOrLeaderService.getUserLeaderByLevel(userId,
                        deptId, props.getLeader().getLevel(), props.getLeader().getSkipEmpty());
                Optional.ofNullable(leaderByLevel).ifPresent(userSet::add);
                break;
            case LEADER_TOP: //用户逐级部门主管
                List<String> leaders = userDeptOrLeaderService.getUserLeadersByLevel(userId, deptId,
                        "TOP".equals(props.getLeaderTop().getEndCondition()) ?
                                0 : props.getLeaderTop().getEndLevel(), props.getLeaderTop().getSkipEmpty());
                Optional.ofNullable(leaders).ifPresent(userSet::addAll);
                break;
            case OTHER_SELECT: //由其他节点指定
                Optional.ofNullable(runtimeService.getVariable(instanceId, nodeId, List.class)).ifPresent(userSet::addAll);
                break;
        }
        //处理审批人为空时，采取默认策略
        if (CollectionUtil.isEmpty(userSet)) {
            switch (props.getNobody().getHandler()) {
                case TO_USER:
                    userSet.addAll(props.getNobody().getAssignedUser().stream().map(OrgUser::getId).collect(Collectors.toList()));
                    break;
                case TO_ADMIN: //TODO 注意系统需要包含该角色 WFLOW_APPROVAL_ADMIN
                    userSet.addAll(userDeptOrLeaderService.getUsersByRoles(CollectionUtil.newArrayList(WflowGlobalVarDef.WFLOW_APPROVAL_ADMIN)));
                    break;
                case TO_PASS:
                    userSet.add(WflowGlobalVarDef.WFLOW_TASK_AGRRE);
                    break;
                case TO_REFUSE:
                    userSet.add(WflowGlobalVarDef.WFLOW_TASK_REFUSE);
                    break;
            }
        } else {
            //将用户替换为当前代理人
            return userDeptOrLeaderService.replaceUserAsAgent(userSet);
        }
        return new ArrayList<>(userSet);
    }

    private Set<String> getDeptUsers(ApprovalProps.DeptProp deptProp, Collection<String> depts) {
        if (Objects.nonNull(deptProp)) {
            switch (deptProp.getType()) {
                case "ALL": //取部门下所有用户
                    return userDeptOrLeaderService.getUsersByDept(depts);
                case "LEADER": //取部门内主管
                    return userDeptOrLeaderService.getLeadersByDept(depts);
                case "ROLE":
                    //取角色相关人员，然后过滤掉不是这些部门内的人
                    return userDeptOrLeaderService.getUsersByRoles(deptProp.getRoles()
                                    .stream().map(OrgUser::getId).collect(Collectors.toList()))
                            .stream().filter(userId -> {
                                for (String dept : depts) {
                                    if (userDeptOrLeaderService.userIsBelongToDept(userId, dept)) {
                                        return true;
                                    }
                                }
                                return false;
                            }).collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    private boolean hasComment(ProcessHandlerParamsVo.ProcessComment comment) {
        return Objects.nonNull(comment) && (StrUtil.isNotBlank(comment.getText())
                || CollectionUtil.isNotEmpty(comment.getAttachments()));
    }

    /**
     * 审批任务，同意、驳回
     *
     * @param task   当前任务
     * @param params 参数
     */
    @Transactional
    public void doApproval(Task task, ProcessHandlerParamsVo params) {
        HashMap<String, Object> var = CollectionUtil.newHashMap(3);
        //检查是否指定节点审批人
        if (CollectionUtil.isNotEmpty(params.getOtherNodeUsers())) {
            //指定审批人
            var.putAll(params.getOtherNodeUsers());
        }
        //var.put("approve", params.getAction().equals(ProcessHandlerParamsVo.Action.agree));
        var.put(WflowGlobalVarDef.TASK_RES_PRE + task.getId(), params.getAction());
        if (StrUtil.isNotBlank(params.getSignature())) {
            var.put("sign_" + task.getId(), params.getSignature());
        }
        //存最近一个审批节点的处理结果
        var.put(WflowGlobalVarDef.PREVIOUS_AP_NODE, task.getTaskDefinitionKey() + ":" + params.getAction());

        //最后一个审批人,审批通过
        ApprovalProps props = JSON.parseObject(
                JSON.toJSONString(runtimeService.getVariable(task.getExecutionId(), WflowGlobalVarDef.LAST_AUDIT_EVENT_TAG)),
                ApprovalProps.class
        );
        if (props != null) {
            final List<String> approvalUsers = getApprovalUsers(task.getExecutionId(),task.getProcessInstanceId(), props);
            if (params.getAction() == ProcessHandlerParamsVo.Action.agree && approvalUsers.contains(task.getAssignee())) {
                var.put(WflowGlobalVarDef.TASK_EVENT_PRE + task.getId(), ProcessHandlerParamsVo.builder().action(params.getAction()).notify(ProcessHandlerParamsVo.Notify.sync_redis_interface).build());
            }
        }
        taskService.complete(params.getTaskId(), var);
    }

    private void doRecallTask(Task task, String userId, ProcessHandlerParamsVo params) {
        //执行自定义回退逻辑
        HashMap<String, Object> var = CollectionUtil.newHashMap(1);
        var.put(WflowGlobalVarDef.TASK_RES_PRE + params.getTaskId(), params.getAction());
        //设置流程存在退回状态标记
        var.put(WflowGlobalVarDef.NODE_RETURN, task.getTaskDefinitionKey());
        runtimeService.setVariables(task.getProcessInstanceId(), var);
        managementService.executeCommand(new RecallToHisApprovalNodeCmd(runtimeService, params.getTaskId(), params.getTargetNode()));
        if ("root".equals(params.getTargetNode())) {
            //如果是退回发起人节点，那么推送一条通知消息，因为发起人节点没有设置任务监听器，所以不会触发UserTaskListener
            ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            notifyService.notify(NotifyDto.builder()
                    .title("您的审批被退回")
                    .instanceId(task.getProcessInstanceId())
                    .target(instance.getStartUserId())
                    .processDefId(task.getProcessDefinitionId())
                    .content(StrUtil.builder("您提交的【", instance.getProcessDefinitionName(),
                            "】被退回，请即时处理").toString())
                    .type(NotifyDto.TypeEnum.WARNING)
                    .build());
        }
        log.info("用户[{}] 退回流程[{}] [{} -> {}]", userId, params.getInstanceId(), task.getTaskDefinitionKey(), params.getTargetNode());
    }

    /**
     * 转交任务处理
     *
     * @param task   当前任务
     * @param params 参数
     * @param userId 任务当前处理人
     */
    private void doTransferTask(Task task, ProcessHandlerParamsVo params, String userId) {
        taskService.setOwner(params.getTaskId(), userId);
        taskService.setAssignee(params.getTaskId(), params.getTargetUser());
        OrgUser orgUser = userDeptOrLeaderService.getUserMapByIds(CollectionUtil.newArrayList(userId)).get(userId);
        notifyService.notify(NotifyDto.builder()
                .title("待处理的转交任务")
                .instanceId(task.getProcessInstanceId())
                .target(params.getTargetUser())
                .nodeId(task.getTaskDefinitionKey())
                .processDefId(task.getProcessDefinitionId())
                .content(StrUtil.builder(orgUser.getName(),
                        "转交了一项任务给您处理").toString())
                .type(NotifyDto.TypeEnum.WARNING)
                .build());
        log.info("[{}]将任务[{}]转交给[{}]处理", userId, task.getId(), params.getTargetUser());
    }

    /**
     * 取消流程处理
     *
     * @param instanceId 流程实例ID
     * @param nodeId     当前流程所处节点ID
     */
    private void doCancelProcess(String instanceId, String nodeId) {
        List<Execution> executions = runtimeService.createExecutionQuery()
                .parentId(instanceId)
                .onlyChildExecutions().list();
        //强制流程指向驳回
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(instanceId)
                .moveActivityIdTo(nodeId, "cancel-end")
                .moveExecutionsToSingleActivityId(executions.stream().map(Execution::getId)
                        .collect(Collectors.toList()), "cancel-end")
                .changeState();
    }

    /**
     * 向上遍历加载用户任务审批节点
     *
     * @param nowNode 当前节点
     * @param list    审批节点集合
     */
    private void loadBeforeSerialUserTaskNode(FlowNode nowNode, Set<String> list, Integer index) {
        //按串行向上遍历
        List<SequenceFlow> incomingFlows = nowNode.getIncomingFlows();
        FlowElement beforeNode = incomingFlows.get(index).getSourceFlowElement();
        if (beforeNode instanceof UserTask) {
            list.add(beforeNode.getId());
            loadBeforeSerialUserTaskNode((FlowNode) beforeNode, list, 0);
        } else if (beforeNode instanceof ParallelGateway || beforeNode instanceof InclusiveGateway) {
            //碰到并行和包容网关，判断是不是合流点，是就绕过继续往上，不是就截止
            if (((Gateway) beforeNode).getOutgoingFlows().size() == 1) {
                //只有一条出线，那么就是合流点
                SequenceFlow sequenceFlow = ((Gateway) beforeNode).getIncomingFlows().get(0);
                FlowElement gateway = getStartGateway(sequenceFlow.getSourceFlowElement(), beforeNode instanceof ParallelGateway);
                if (Objects.nonNull(gateway)) {
                    loadBeforeSerialUserTaskNode((FlowNode) gateway, list, 0);
                }
            }
        } else if (beforeNode instanceof ExclusiveGateway) {
            //碰到排他网关，里面每一条分支都要遍历
            List<SequenceFlow> flows = ((ExclusiveGateway) beforeNode).getIncomingFlows();
            for (int i = 0; i < flows.size(); i++) {
                loadBeforeSerialUserTaskNode((FlowNode) beforeNode, list, i);
            }
        } else if (!(beforeNode instanceof StartEvent)) {
            //遍历直到根节点
            loadBeforeSerialUserTaskNode((FlowNode) beforeNode, list, 0);
        }
    }

    /**
     * 获取开始的并行/包容网关节点
     *
     * @param node  节点
     * @param isPar 是否为并行网关
     * @return 开始的网关
     */
    private FlowElement getStartGateway(FlowElement node, Boolean isPar) {
        if ((isPar && node instanceof ParallelGateway) || (!isPar && node instanceof InclusiveGateway)) {
            //找到了对应的网关起始节点
            return node;
        }
        FlowNode flowNode = (FlowNode) node;
        return getStartGateway(flowNode.getIncomingFlows().get(0).getSourceFlowElement(), isPar);
    }
}

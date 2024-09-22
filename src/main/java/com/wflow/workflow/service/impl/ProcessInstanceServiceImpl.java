package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wflow.bean.FlowProcessContext;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.WflowCcTasks;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.*;
import com.wflow.service.OrgRepositoryService;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.UELTools;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.OperationPerm;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.enums.ApprovalModeEnum;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.bean.process.enums.ProcessResultEnum;
import com.wflow.workflow.bean.process.form.Form;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.bean.process.props.RootProps;
import com.wflow.workflow.bean.vo.*;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.extension.cmd.StartProcessInstanceCmdN;
import com.wflow.workflow.service.*;
import com.wflow.workflow.utils.Executor;
import lombok.extern.slf4j.Slf4j;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.SystemUtils.getUserName;

/**
 * @author : JoinFyc
 * @date : 2024/8/23
 */
@Slf4j
@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    @Autowired
    private WflowModelsMapper modelsMapper;

    @Autowired
    private WflowModelHistorysMapper modelHistorysMapper;

    @Autowired
    private ProcessNodeCacheService nodeCatchService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserDeptOrLeaderService userDeptOrLeaderService;

    @Autowired
    private ProcessTaskService processTaskService;

    @Autowired
    private WflowCcTasksMapper ccTasksMapper;

    @Autowired
    private WflowSubProcessMapper subProcessMapper;

    @Autowired
    private BusinessDataStorageService businessDataService;

    @Autowired
    private UELTools uelTools;


    @Override
    @Transactional
    public String startProcess(String defId, ProcessStartParamsVo params) {
        Map<String, Object> processVar = new HashMap<>();
        processVar.putAll(params.getFormData());
        processVar.putAll(params.getProcessUsers());
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(defId).latestVersion().singleResult();
        if (ObjectUtil.isNotNull(processDefinition) && processDefinition.isSuspended()) {
            throw new BusinessException("流程未启用，请先启用");
        }
        //设置发起的人员及部门信息
        String userId = getUserId();
        //设置发起人部门ID，此处减小流程变量表数据改成只放ID
        processVar.put(WflowGlobalVarDef.START_DEPT, params.getDeptId());
        WflowModels wflowModels = modelsMapper.selectOne(new LambdaQueryWrapper<WflowModels>().eq(WflowModels::getProcessDefId, defId));
        Map<String, ProcessNode<?>> nodeMap = nodeCatchService.reloadProcessByStr(wflowModels.getProcess());
        Map<String, Object> propsMap = nodeMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                v -> null == v.getValue().getProps() ? new HashMap<>() : v.getValue().getProps()));
        //将表单及流程配置设置为变量，跟随版本
        processVar.put(WflowGlobalVarDef.WFLOW_NODE_PROPS, propsMap);
        processVar.put(WflowGlobalVarDef.WFLOW_FORMS, JSONArray.parseArray(wflowModels.getFormItems(), Form.class));
        processVar.put(WflowGlobalVarDef.INITIATOR, userId);
        processVar.put(WflowGlobalVarDef.FLOW_UNIQUE_ID, params.getFlowUniqueId());
        //最终审批人的事件Key
        final ProcessNode<?> lastNode = Objects.requireNonNull(nodeMap.entrySet().stream().reduce((first, second) -> second).orElse(null)).getValue();
        processVar.put(WflowGlobalVarDef.LAST_AUDIT_EVENT_TAG,lastNode.getProps());

        //构造流程实例名称
        final UserDo user = orgRepositoryService.getUserById(userId);
        if(user == null){throw new BusinessException("用户不存在"); }
        String instanceName = user.getUserName() + "发起的" + processDefinition.getName();
        //这样做貌似无效果，变量表不会多INITIATOR变量，但是流程表发起人有效
        //TODO 流程开启 start
        Authentication.setAuthenticatedUserId(userId);
        ProcessInstance processInstance = managementService.executeCommand(new StartProcessInstanceCmdN<>(
                instanceName, defId, null, processVar, UserUtil.getTenant().getTenantId()));
        businessDataService.saveInstanceFormData(processInstance.getProcessInstanceId(), params.getFormData());
        log.info("启动 {} 流程实例 {} 成功", processInstance.getProcessDefinitionName(), processInstance.getProcessInstanceId());
        //自动完成发起人节点任务，发起人是一个UserTask，发起后触发start事件然后分配Task给发起人，所以这里要自动完成
        Task rootTask = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).active().singleResult();
        if (Objects.nonNull(rootTask)) {
            rootTask.setDescription(ProcessHandlerParamsVo.Action.complete.toString());
            taskService.complete(rootTask.getId());
        }
        Authentication.setAuthenticatedUserId(null);
        return processInstance.getProcessInstanceId();
    }

    @Override
    public String getBeforeTask(String instanceId, String task) {
        return null;
    }

    @Override
    @Transactional
    public void delProcessInstance(String instanceId) {
        // 删除流程实例
        try {
            runtimeService.deleteProcessInstance(instanceId, "删除");
        } catch (Exception ignored) {
        }
        // 删除流程历史实例
        historyService.deleteHistoricProcessInstance(instanceId);
        //删除的同时相关抄送表数据也要删除，不然分页数据会错
        ccTasksMapper.delete(new LambdaQueryWrapper<WflowCcTasks>().eq(WflowCcTasks::getInstanceId, instanceId));
        //删除物理表表单数据及修改记录
        businessDataService.deleteFormData(instanceId);
        log.info("删除流程实例[{}]成功", instanceId);
    }

    @Override
    public ProcessProgressVo getInstanceProgress(String nodeId, String instanceId) {
        //构建处理参数
        final ProcessProgressVo.ProcessProgressVoBuilder builder = ProcessProgressVo.builder();
        Map<String, Object> ruVariables = runtimeService.getVariables(instanceId);
        String flowUniqueId = (String)ruVariables.get(WflowGlobalVarDef.FLOW_UNIQUE_ID);
        builder.flowUniqueId(flowUniqueId);

        //最后一个审批人标识
        Object o = ruVariables.get(WflowGlobalVarDef.LAST_AUDIT_EVENT_TAG);
        ApprovalProps props = JSON.parseObject(JSON.toJSONString(o), ApprovalProps.class);
        final List<String> approvalUsers = processTaskService.getApprovalUsers(instanceId,nodeId, props);
        if(approvalUsers.contains(UserUtil.getLoginUserId())){builder.lastAudit(true);}

        //先查实例，然后判断是子流程还是主流程
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult();
        //数据分类 表单配置及数据、审批任务结果、
        ProcessInstanceOwnerDto owner = null;
        List<Form> forms = Collections.emptyList();
        Map<String, Object> formDatas = new HashMap<>();
        Map<String, ProcessHandlerParamsVo.Action> approvalResults = new HashMap<>();
        Map<String, Object> nodeProps = new HashMap<>();
        Map<String, String> signs = new HashMap<>();
        //是否是子流程
        boolean isSub = StrUtil.isNotBlank(instance.getSuperProcessInstanceId());
        HistoricProcessInstance mainInst = isSub ? null : instance;
        if (isSub) {
            //查出主流程表单数据
            mainInst = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(instance.getSuperProcessInstanceId()).singleResult();
            formDatas = businessDataService.getProcessInstanceFormData(mainInst.getId());
        }
        //优化查询，把之前查好几次的一次性查出来然后再分类
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instanceId).executionId(instanceId).list();
        Map<String, Object> vars = Objects.nonNull(instance.getEndTime()) ?
                new HashMap<>() : uelTools.getContextVar(instanceId, instance.getProcessDefinitionId());
        //遍历所有变量，将数据分类
        for (HistoricVariableInstance var : variables) {
            vars.put(var.getVariableName(), var.getValue());
            if (var.getVariableName().startsWith(WflowGlobalVarDef.TASK_RES_PRE)) {
                approvalResults.put(var.getVariableName(), (ProcessHandlerParamsVo.Action) var.getValue());
            } else if (!isSub && var.getVariableName().startsWith("field")) {
                formDatas.put(var.getVariableName(), var.getValue());
            } else if (var.getVariableName().startsWith("sign_")) {
                signs.put(var.getVariableName().substring(5), String.valueOf(var.getValue()));
            } else if (!isSub && WflowGlobalVarDef.WFLOW_FORMS.equals(var.getVariableName())) {
                forms = (List<Form>) var.getValue();
            } else if (WflowGlobalVarDef.WFLOW_NODE_PROPS.equals(var.getVariableName())) {
                nodeProps = (Map<String, Object>) var.getValue();
            } else if (WflowGlobalVarDef.OWNER.equals(var.getVariableName())) {
                owner = (ProcessInstanceOwnerDto) var.getValue();
            } else if (WflowGlobalVarDef.START_DEPT.equals(var.getVariableName())) {
                String key = instance.getStartUserId() + "_" + var.getValue();
                Map<String, UserDeptDo> infoMap = orgRepositoryService.getUserDeptInfos(CollectionUtil.newArrayList(key));
                UserDeptDo userDeptDo = infoMap.getOrDefault(key, new UserDeptDo());
                owner = ProcessInstanceOwnerDto.builder().ownerDeptId(String.valueOf(var.getValue()))
                        .owner(instance.getStartUserId()).ownerName(userDeptDo.getUserName())
                        .ownerDeptId(userDeptDo.getDeptId()).ownerDeptName(userDeptDo.getDeptName()).build();
            }
        }
        ProcessNode<?> currentNode = null;
        String modelFormConfig = null;
        ProcessProgressVo.InstanceExternSetting externSetting = null;
        OperationPerm operationPerms = null;
        Map<String, ProcessNode<?>> nodeMap = Collections.emptyMap();
        if (isSub) {
            WflowSubProcess subProcess = subProcessMapper.selectOne(new LambdaQueryWrapper<>(WflowSubProcess.builder()
                    .procDefId(instance.getProcessDefinitionId()).build()));
            nodeMap = nodeCatchService.reloadProcessByStr(subProcess.getProcess());
            currentNode = nodeMap.get(nodeId);
            operationPerms = getOperationPerm(currentNode);
            HistoricVariableInstance formsVar = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(mainInst.getId()).variableName(WflowGlobalVarDef.WFLOW_FORMS).singleResult();
            forms = (List<Form>) formsVar.getValue();
            //获取主流程节点配置信息
        }
        if (StrUtil.isNotBlank(nodeId)) {
            //搜索当前版本流程的配置
            WflowModelHistorys modelHistory = modelHistorysMapper.selectOne(new LambdaQueryWrapper<>(WflowModelHistorys.builder()
                    .processDefId(mainInst.getProcessDefinitionId()).version(mainInst.getProcessDefinitionVersion()).build()));
            nodeMap = nodeCatchService.reloadProcessByStr(modelHistory.getProcess());
            currentNode = nodeMap.get(isSub ? instance.getBusinessKey() : nodeId);
            if (!isSub) {
                operationPerms = getOperationPerm(currentNode);
            }
            modelFormConfig = modelHistory.getFormConfig();
            externSetting = getExternSetting(modelHistory.getSettings(), instance.getStartUserId());
        }
        //表单的 currentNode 以主流程为准
        List<Form> formItems = businessDataService.filterFormAndDataByPermConfig(forms, formDatas, currentNode);
        //下面都是一样的
        UserDo users = orgRepositoryService.getUserById(instance.getStartUserId());
        OrgUser startUser = OrgUser.builder().id(users.getUserId()).name(users.getUserName()).avatar(users.getAvatar()).build();
        List<ProcessProgressVo.ProgressNode> taskRecords = getHisTaskRecords(instanceId, nodeProps, approvalResults, signs, instance.getEndTime());
        //添加抄送
        taskRecords.addAll(getCcTaskRecords(instanceId));
        if (ObjectUtil.isNull(instance.getEndTime())) {
            taskRecords.addAll(processTaskService.getFutureTask(instance, owner.getOwnerDeptId(), vars, nodeMap));
        }
        //按开始时间对节点进行排序
        taskRecords = taskRecords.stream()
                .sorted(Comparator.comparing(ProcessProgressVo.ProgressNode::getStartTime))
                .collect(Collectors.toList());
        ProcessProgressVo.ProgressNode progressNode = taskRecords.stream()
                .filter(n -> "root".equals(n.getNodeId()) && Objects.isNull(n.getResult()))
                .findFirst().orElse(null);
        if (Objects.nonNull(progressNode)) {
            //辅助前端UI构建，第一条一般都是
            progressNode.setResult(ProcessHandlerParamsVo.Action.complete);
            progressNode.setNodeType(NodeTypeEnum.ROOT);
        } else {
            //没有就进行构建，兼容之前的流程
            taskRecords.add(0, ProcessProgressVo.ProgressNode.builder()
                    .nodeId("root")
                    .name(isSub ? "发起子流程" : "提交申请")
                    .user(startUser)
                    .nodeType(NodeTypeEnum.ROOT)
                    .startTime(instance.getStartTime())
                    .finishTime(instance.getStartTime())
                    .taskId("root")
                    .result(ProcessHandlerParamsVo.Action.complete)
                    .build());
        }
        //提取全量表单数据
        if (StrUtil.isBlank(modelFormConfig)) {
            WflowModelHistorys modelHistory = modelHistorysMapper.selectOne(new LambdaQueryWrapper<>(WflowModelHistorys.builder()
                    .processDefId(mainInst.getProcessDefinitionId()).version(mainInst.getProcessDefinitionVersion()).build()));
            modelFormConfig = modelHistory.getFormConfig();
            externSetting = getExternSetting(modelHistory.getSettings(), instance.getStartUserId());
        }
        ProcessResultEnum processResult = ProcessResultEnum.resolveResult(instance.getEndActivityId());
        return builder
                .instanceId(instanceId)
                .version(instance.getProcessDefinitionVersion())
                .formItems(formItems)
                .formConfig(JSONObject.parseObject(modelFormConfig))
                .formData(formDatas)
                .processDefName(instance.getProcessDefinitionName())
                .staterUser(startUser)
                .starterDept(null == owner ? null : owner.getOwnerDeptName())
                .status(processResult.getDesc())
                .result(processResult)
                .startTime(instance.getStartTime())
                .finishTime(instance.getEndTime())
                .progress(taskRecords)
                .operationPerm(operationPerms)
                .externSetting(externSetting)
                .build();
    }

    /**
     * 获取流程的审批历史记录
     *
     * @param instanceId 审批实例ID
     * @param nodeProps  节点设置
     * @param varMap     变量
     * @param signs      签名
     * @param instEndTime 流程实例结束时间
     * @return 历史记录列表
     */
    private List<ProcessProgressVo.ProgressNode> getHisTaskRecords(String instanceId, Map<String, Object> nodeProps,
                                                                   Map<String, ProcessHandlerParamsVo.Action> varMap,
                                                                   Map<String, String> signs, Date instEndTime) {
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceId).orderByHistoricActivityInstanceStartTime().asc().list();
        Set<String> userSet = new HashSet<>();
        //获取节点处理结果
        Map<String, List<TaskCommentVo>> commentMap = new HashMap<>();
        //统一处理所有评论数据，省的多次查询
        List<TaskCommentVo> cmvos = taskService.getProcessInstanceComments(instanceId).stream().map(comment -> {
            userSet.add(comment.getUserId());
            TaskCommentVo commentVo = TaskCommentVo.builder()
                    .id(comment.getId())
                    .taskId(comment.getTaskId())
                    .commentType(comment.getType())
                    .type("COMMENT")
                    .createTime(comment.getTime())
                    .user(OrgUser.builder().id(comment.getUserId()).build())
                    .build();
            ProcessHandlerParamsVo.ProcessComment processComment = JSONObject.parseObject(comment.getFullMessage(), ProcessHandlerParamsVo.ProcessComment.class);
            commentVo.setText(processComment.getText());
            commentVo.setAttachments(processComment.getAttachments());
            return commentVo;
        }).collect(Collectors.toList());
        cmvos.forEach(cm -> {
            //把评论数据按照task进行归类
            String taskId = Optional.ofNullable(cm.getTaskId()).orElse(instanceId);
            List<TaskCommentVo> vos = commentMap.computeIfAbsent(taskId, k -> new ArrayList<>());
            vos.add(cm);
        });
        List<ProcessProgressVo.ProgressNode> progressNodes = list.stream()
                .filter(his -> ObjectUtil.isNotNull(his.getTaskId()) || "callActivity".equals(his.getActivityType()))
                .map(his -> {
                    Object props = nodeProps.get(his.getActivityId());
                    List<TaskCommentVo> commentVos = commentMap.getOrDefault(his.getTaskId(), Collections.emptyList());
                    ProcessProgressVo.ProgressNode node = ProcessProgressVo.ProgressNode.builder()
                            .nodeId(his.getActivityId())
                            .isFuture(false)
                            .name(his.getActivityName())
                            .startTime(his.getStartTime())
                            .finishTime(Optional.ofNullable(his.getEndTime()).orElse(instEndTime))
                            .comment(commentVos).build();
                    if ("callActivity".equals(his.getActivityType())) {
                        node.setNodeType(NodeTypeEnum.SUBPROC);
                        //取子流程实例
                        HistoricProcessInstance subInst = historyService.createHistoricProcessInstanceQuery()
                                .processInstanceId(his.getCalledProcessInstanceId())
                                .singleResult();
                        userSet.add(subInst.getStartUserId());
                        node.setUser(OrgUser.builder().id(subInst.getStartUserId()).build());
                        node.setTaskId(subInst.getId());
                        ProcessResultEnum result = ProcessResultEnum.resolveResult(subInst.getEndActivityId());
                        switch (result) {
                            case PASS:
                                node.setResult(ProcessHandlerParamsVo.Action.agree);
                                break;
                            case REFUSE:
                                node.setResult(ProcessHandlerParamsVo.Action.refuse);
                                break;
                            case CANCEL:
                                node.setResult(ProcessHandlerParamsVo.Action.cancel);
                                break;
                        }
                    } else {
                        ApprovalModeEnum approvalMode = null;
                        if (props instanceof ApprovalProps) {
                            approvalMode = ((ApprovalProps) props).getMode();
                        }
                        userSet.add(his.getAssignee());
                        node.setNodeType(NodeTypeEnum.APPROVAL);
                        node.setApprovalMode(approvalMode);
                        node.setUser(OrgUser.builder().id(his.getAssignee()).build());
                        node.setTaskId(his.getTaskId());
                        node.setSignature(signs.get(his.getTaskId()));
                        node.setResult(varMap.get(WflowGlobalVarDef.TASK_RES_PRE + his.getTaskId()));
                    }
                    return node;
                }).collect(Collectors.toList());
        //将非任务类的评论转换成流程节点
        progressNodes.addAll(commentMap.getOrDefault(instanceId, Collections.emptyList()).stream().map(cm ->
                ProcessProgressVo.ProgressNode.builder()
                        .nodeId(cm.getId())
                        .name("参与评论")
                        .user(cm.getUser())
                        .startTime(cm.getCreateTime())
                        .finishTime(cm.getCreateTime())
                        .taskId(cm.getId())
                        .comment(CollectionUtil.newArrayList(cm))
                        .result(ProcessHandlerParamsVo.Action.comment)
                        .build()).collect(Collectors.toList()));
        if (CollectionUtil.isNotEmpty(userSet)) {
            //过滤掉系统节点
            Map<String, OrgUser> map = userDeptOrLeaderService.getUserMapByIds(userSet.stream()
                    .filter(v -> !WflowGlobalVarDef.WFLOW_TASK_AGRRE.equals(v) && !WflowGlobalVarDef.WFLOW_TASK_REFUSE.equals(v))
                    .collect(Collectors.toSet()));
            progressNodes.forEach(n -> {
                if (WflowGlobalVarDef.WFLOW_TASK_AGRRE.equals(n.getUser().getId())
                        || WflowGlobalVarDef.WFLOW_TASK_REFUSE.equals(n.getUser().getId())) {
                    n.setUser(WflowGlobalVarDef.SYS);
                } else {
                    n.setUser(map.get(n.getUser().getId()));
                    n.getComment().forEach(c -> c.setUser(map.get(c.getUser().getId())));
                }
            });
        }
        return progressNodes;
    }

    @Override
    public List<Task> getProcessInstanceTaskList(String instanceId) {
        return taskService.createTaskQuery().processInstanceId(instanceId).active().list();
    }

    @Override
    public Page<ProcessInstanceVo> getUserSubmittedList(Integer pageSize, Integer pageNo, String startUser, String code,
                                                        Boolean finished, String[] startTimes, String keyword, String fieldId, String fieldVal) {

        HistoricProcessInstanceQuery instanceQuery = historyService.createHistoricProcessInstanceQuery();
        Executor.builder()
                .ifNotBlankNext(startUser, instanceQuery::startedBy)
                .ifNotBlankNext(code, instanceQuery::processDefinitionKey)
                .ifTrueNext(null != startTimes && startTimes.length > 1, () -> {
                    instanceQuery.startedAfter(DateUtil.parse(startTimes[0]));
                    instanceQuery.startedBefore(DateUtil.parse(startTimes[1]));
                })
                .ifNotBlankNext(keyword, v -> instanceQuery.processInstanceNameLike(StrUtil.format("%{}%", keyword)))
                .ifTrueNext(Boolean.TRUE.equals(finished), instanceQuery::finished)
                .ifTrueNext(Boolean.FALSE.equals(finished), instanceQuery::unfinished)
                .ifNotBlankNext(fieldId, id -> {
                    if (StrUtil.isBlank(code)){
                        throw new BusinessException("搜索表单值必须先指定表单流程类型");
                    }
                    if (StrUtil.isNotBlank(fieldVal)){
                        instanceQuery.variableValueLike(fieldId, "%" + fieldVal + "%");
                    }
                });
        List<HistoricProcessInstance> historicProcessInstances = instanceQuery
                .orderByProcessInstanceStartTime().desc()
                .orderByProcessInstanceEndTime().desc()
                .listPage(pageSize * (pageNo - 1), pageSize);
        Page<ProcessInstanceVo> page = new Page<>();
        page.setTotal(instanceQuery.count());
        page.setCurrent(pageNo);
        page.setSize(pageSize);
        Map<String, String> instanceNodeMap = new HashMap<>();
        page.setRecords(getInstances(instanceNodeMap, historicProcessInstances));
        return page;
    }

    @Override
    public Page<ProcessInstanceVo> getCcMeInstance(Integer pageSize, Integer pageNo, String code, String[] startTimes) {
        LambdaQueryWrapper<WflowCcTasks> queryWrapper = new LambdaQueryWrapper<WflowCcTasks>()
                .eq(WflowCcTasks::getUserId, this.getUserId());
        HistoricProcessInstanceQuery instanceQuery = historyService.createHistoricProcessInstanceQuery();
        Executor.builder()
                .ifNotBlankNext(code, v -> queryWrapper.eq(WflowCcTasks::getCode, code))
                .ifNotBlankNext(code, instanceQuery::processDefinitionKey)
                .ifTrueNext(null != startTimes && startTimes.length > 1, () -> {
                    queryWrapper.ge(WflowCcTasks::getCreateTime, DateUtil.parse(startTimes[0]));
                    queryWrapper.le(WflowCcTasks::getCreateTime, DateUtil.parse(startTimes[1]));
                });
        Page<WflowCcTasks> tasks = ccTasksMapper.selectPage(new Page<>(pageNo, pageSize),
                queryWrapper.orderByDesc(WflowCcTasks::getCreateTime));
        Map<String, String> instanceMap = tasks.getRecords().stream().collect(
                Collectors.toMap(WflowCcTasks::getInstanceId, WflowCcTasks::getNodeId, (v1, v2) -> v2));
        Page<ProcessInstanceVo> page = new Page<>();
        page.setCurrent(pageNo);
        page.setSize(pageSize);

        if (CollectionUtil.isNotEmpty(instanceMap)) {
            List<HistoricProcessInstance> historicProcessInstances = instanceQuery
                    .processInstanceIds(instanceMap.keySet())
                    .orderByProcessInstanceStartTime().desc()
                    .orderByProcessInstanceEndTime().desc().list();
            page.setTotal(tasks.getTotal());
            page.setRecords(getInstances(instanceMap, historicProcessInstances));
        } else {
            page.setTotal(0);
            page.setRecords(Collections.emptyList());
        }
        return page;
    }

    /**
     * 获取用户ID
     */
    protected static String getUserId() {
        return FlowProcessContext.getFlowProcessContext() == null ? UserUtil.getLoginUserId() : FlowProcessContext.getFlowProcessContext().getUserId();
    }

    @Override
    public InstanceCountVo getProcessInstanceCount() {
        String userId = UserUtil.getLoginUserId();
        Long cc = ccTasksMapper.selectCount(new LambdaQueryWrapper<WflowCcTasks>().eq(WflowCcTasks::getUserId, userId));
        Long todo = taskService.createTaskQuery().taskCandidateOrAssigned(userId).count();
        Long mySubmited = historyService.createHistoricProcessInstanceQuery().startedBy(userId).unfinished().count();
        return InstanceCountVo.builder().todo(todo).mySubmited(mySubmited).cc(cc.intValue()).build();
    }

    /**
     * 获取抄送的流程实例信息
     *
     * @param instanceId 实例ID
     * @return 抄送我的流程
     */
    private List<ProcessProgressVo.ProgressNode> getCcTaskRecords(String instanceId) {
        Set<String> ccUsers = new HashSet<>();
        List<ProcessProgressVo.ProgressNode> ccList = ccTasksMapper.selectList(new LambdaQueryWrapper<WflowCcTasks>()
                .eq(WflowCcTasks::getInstanceId, instanceId)).stream().map(task -> {
            ccUsers.add(task.getUserId());
            return ProcessProgressVo.ProgressNode.builder()
                    .nodeId(task.getNodeId())
                    .nodeType(NodeTypeEnum.CC)
                    .isFuture(false)
                    .name(task.getNodeName())
                    .comment(Collections.emptyList())
                    .user(OrgUser.builder().id(task.getUserId()).build())
                    .startTime(task.getCreateTime())
                    .finishTime(task.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(ccUsers)) {
            Map<String, OrgUser> userMap = userDeptOrLeaderService.getUserMapByIds(ccUsers);
            ccList.stream().peek(v -> v.setUser(userMap.get(v.getUser().getId()))).collect(Collectors.toList());
        }
        return ccList;
    }

    /**
     * 构造流程实例列表
     *
     * @param instanceNodeMap 流程实例ID与对应节点ID映射
     * @param Instances       流程实例
     * @return 流程实例列表
     */
    private List<ProcessInstanceVo> getInstances(Map<String, String> instanceNodeMap, List<HistoricProcessInstance> Instances) {
        Set<String> staterUsers = new HashSet<>();
        //获取表单摘要数据
        Map<String, List<FormAbstractsVo>> formAbstractsVoMap = businessDataService.getInstanceAbstractDatas(Instances.stream()
                .collect(Collectors.toMap(HistoricProcessInstance::getId, HistoricProcessInstance::getProcessDefinitionId)));
        //构造流程实例列表数据
        Map<String, ProcessInstanceVo> runInst = new HashMap<>(instanceNodeMap.size());
        List<ProcessInstanceVo> instanceVos = Instances.stream().map(ist -> {
            staterUsers.add(ist.getStartUserId());
            ProcessResultEnum processResult = ProcessResultEnum.resolveResult(ist.getEndActivityId());
            ProcessInstanceVo instanceVo = ProcessInstanceVo.builder()
                    .processDefId(ist.getProcessDefinitionId())
                    .instanceId(ist.getId())
                    .instanceName(ist.getName())
                    .superInstanceId(Optional.ofNullable(ist.getSuperProcessInstanceId()).orElse(ist.getId()))
                    .nodeId(instanceNodeMap.get(ist.getId()))
                    .formId(ist.getProcessDefinitionKey())
                    .staterUserId(ist.getStartUserId())
                    .startTime(ist.getStartTime())
                    .finishTime(ist.getEndTime())
                    .processDefName(ist.getProcessDefinitionName())
                    .status(processResult.getDesc())
                    .result(processResult)
                    .version(ist.getProcessDefinitionVersion())
                    .formAbstracts(formAbstractsVoMap.getOrDefault(ist.getId(), Collections.emptyList()))
                    .build();
            if (ProcessResultEnum.RUNNING.equals(instanceVo.getResult())) {
                //没有结束，还在走流程，获取任务
                runInst.put(ist.getId(), instanceVo);
            } else {
                instanceVo.setTaskName(instanceVo.getResult().getDesc());
            }
            return instanceVo;
        }).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(runInst.keySet())) {
            taskService.createTaskQuery().processInstanceIdIn(runInst.keySet()).active().list().stream()
                    .collect(Collectors.groupingBy(Task::getProcessInstanceId)).forEach((istId, tasks) -> {
                        Optional.ofNullable(runInst.get(istId)).ifPresent(ist -> {
                            ist.setNodeId(Optional.ofNullable(ist.getNodeId()).orElseGet(() -> {
                                if (CollectionUtil.isNotEmpty(tasks)) {
                                    return tasks.get(0).getTaskDefinitionKey();
                                }
                                return null;
                            }));
                            ist.setTaskName(StrUtil.join("、", tasks.stream().map(TaskInfo::getName).collect(Collectors.toSet())));
                        });
                    });
        }
        final FlowProcessContext flowProcessContext = FlowProcessContext.getFlowProcessContext();
        if (flowProcessContext != null && flowProcessContext.getFieldTag() == Boolean.TRUE) {
            return instanceVos;
        }
        if (CollectionUtil.isNotEmpty(staterUsers)) {
            Map<String, OrgUser> userMap = userDeptOrLeaderService.getUserMapByIds(staterUsers);
            return instanceVos.stream().map(v -> {
                v.setStaterUser(userMap.get(v.getStaterUserId()));
                return v;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 获取流程处理操作权限
     *
     * @param currentNode 当前的节点
     * @return 操作权限设置
     */
    private OperationPerm getOperationPerm(ProcessNode<?> currentNode) {
        OperationPerm operationPerm = null;
        if (Objects.nonNull(currentNode)) {
            if (NodeTypeEnum.ROOT.equals(currentNode.getType())) {
                RootProps props = (RootProps) currentNode.getProps();
                operationPerm = props.getOperationPerm();
                if (Objects.isNull(operationPerm)) {
                    operationPerm = OperationPerm.builder()
                            .complete(new OperationPerm.Operations("提交", true))
                            .build();
                }else {
                    OperationPerm.Operations agree = operationPerm.getAgree();
                    if (Objects.nonNull(agree) && Objects.isNull(operationPerm.getComplete())){
                        agree.setAlisa("提交");
                        operationPerm.setComplete(agree);
                    }
                    operationPerm.setAgree(null);
                }
            } else if (NodeTypeEnum.APPROVAL.equals(currentNode.getType())) {
                ApprovalProps props = (ApprovalProps) currentNode.getProps();
                operationPerm = props.getOperationPerm();
                if (Objects.isNull(operationPerm)) {
                    operationPerm = OperationPerm.builder()
                            .agree(new OperationPerm.Operations("同意", true))
                            .refuse(new OperationPerm.Operations("拒绝", true))
                            .transfer(new OperationPerm.Operations("转交", true))
                            .afterAdd(new OperationPerm.Operations("加签", true))
                            .recall(new OperationPerm.Operations("退回", true))
                            .build();
                }
            } else if (NodeTypeEnum.TASK.equals(currentNode.getType())) {
                ApprovalProps props = (ApprovalProps) currentNode.getProps();
                operationPerm = props.getOperationPerm();
                if (Objects.isNull(operationPerm)) {
                    operationPerm = OperationPerm.builder()
                            .complete(new OperationPerm.Operations("提交", true))
                            .transfer(new OperationPerm.Operations("转办", true))
                            .afterAdd(new OperationPerm.Operations("加签", true))
                            .recall(new OperationPerm.Operations("退回", true))
                            .build();
                }else {
                    OperationPerm.Operations agree = operationPerm.getAgree();
                    if (Objects.nonNull(agree) && Objects.isNull(operationPerm.getComplete())){
                        agree.setAlisa("提交");
                        operationPerm.setComplete(agree);
                    }
                    operationPerm.setAgree(null);
                }
            }
        }
        return operationPerm;
    }

    private ProcessProgressVo.InstanceExternSetting getExternSetting(String setting, String startUser) {
        JSONObject parsed = JSONObject.parseObject(setting);
        return ProcessProgressVo.InstanceExternSetting.builder()
                .enableSign(parsed.getBoolean("sign"))
                .enableCancel(Boolean.TRUE.equals(parsed.getBoolean("enableCancel"))
                        && startUser.equals(UserUtil.getLoginUserId()))
                .build();
    }
}

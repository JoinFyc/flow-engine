package com.wflow.workflow;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.enums.ApprovalTypeEnum;
import com.wflow.workflow.bean.process.enums.ConditionModeEnum;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.bean.process.props.*;
import com.wflow.workflow.config.callActivity.WflowCallActivityBehavior;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.task.ApprovalTimeoutServiceTask;
import com.wflow.workflow.task.CcServiceTask;
import com.wflow.workflow.task.SubProcessInitTask;
import com.wflow.workflow.task.TriggerServiceTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * json -> bpmn核心转换器
 * @author : willian fu
 * @date : 2022/8/19
 */
@Slf4j
public class WFlowToBpmnCreator {

    //流程元素与ID映射
    private final Map<String, FlowElement> elementMap = new LinkedHashMap<>();
    //节点Map映射，提高取效率
    @Getter
    private final Map<String, ProcessNode<?>> nodeMap = new LinkedHashMap<>();
    //分支栈
    private final Stack<ProcessNode<?>> currentBranchStack = new Stack<>();
    //支路->该支路末端节点
    private final Map<String, List<String>> footerNode = new LinkedHashMap<>();
    //正常完成结束事件
    private final EndEvent endNode = new EndEvent();
    //被驳回的结束事件
    private final EndEvent refuseEndNode = new EndEvent();
    //取消流程的结束事件
    private final EndEvent cancelEndNode = new EndEvent();

    private boolean isSub;

    private final static List<FlowableListener> taskListeners = new ArrayList<>();
    private final static List<FlowableListener> nodeListeners = new ArrayList<>();
    static {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        FlowableListener taskListener = new FlowableListener();
        taskListener.setEvent("all");
        taskListener.setImplementationType("delegateExpression");
        taskListener.setImplementation("${userTaskListener}");
        taskListeners.add(taskListener);

        FlowableListener nodeListener = new FlowableListener();
        nodeListener.setEvent("end");
        nodeListener.setImplementationType("delegateExpression");
        nodeListener.setImplementation("${userTaskListener}");
        nodeListeners.add(nodeListener);
    }

    /**
     * wflow -> bpmnModel 转换
     * @param id 表单流程模型id
     * @param name 流程名
     * @param root 根节点
     * @param isSub 是否是子流程
     * @return 返回转换后的bpmnModel数据
     */
    public BpmnModel loadBpmnFlowXmlByProcess(String id, String name, ProcessNode<?> root, boolean isSub){
        this.isSub = isSub;
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        startEvent.setName("开始");
        loadProcess(root);
        loadProcessEndNode();
        //构建流程
        Process process = new Process();
        process.setId(id);
        process.setName(name);
        process.addFlowElement(startEvent);
        //将组件添加到流程节点
        elementMap.values().forEach(process::addFlowElement);
        //将结束事件连接到主干流程最后一个节点
        Optional<String> lastNode = nodeMap.values().stream()
                .skip(nodeMap.values().size() - 1)
                .map(ProcessNode::getId).findFirst();
        lastNode.ifPresent(node -> {
            SequenceFlow line = createdConnectLine(node, endNode.getId());
            FlowElement element = elementMap.get(node);
            if (element instanceof Gateway){
                ((Gateway) element).setOutgoingFlows(CollectionUtil.newArrayList(line));
            }
            process.addFlowElement(line);
        });
        process.addFlowElement(this.endNode);
        process.addFlowElement(cancelEndNode);
        process.addFlowElement(refuseEndNode);
        //构建Bpmn模型
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        // Bpmn xml自动生成布局及布局节点位置
        new BpmnAutoLayout(bpmnModel).execute();
        //log.info("构建审批流程[{}] 的 xml为：{}", name, xmlStr);
        return bpmnModel;
    }

    /**
     * 加载结束节点
     */
    private void loadProcessEndNode(){
        endNode.setId("process-end");
        endNode.setName("审批流程结束");
        cancelEndNode.setId("cancel-end");
        cancelEndNode.setName("审批流程撤消");
        refuseEndNode.setId("refuse-end");
        TerminateEventDefinition eventDefinition = new TerminateEventDefinition();
        eventDefinition.setTerminateAll(true);
        cancelEndNode.setEventDefinitions(CollectionUtil.newArrayList(eventDefinition));
        //强制终止流程
        TerminateEventDefinition eventDefinition2 = new TerminateEventDefinition();
        eventDefinition2.setTerminateAll(true);
        refuseEndNode.setEventDefinitions(CollectionUtil.newArrayList(eventDefinition2));
        refuseEndNode.setName("审批流程被驳回");
    }

    /**
     * 节点props属性强制转换
     * @param node 节点
     */
    public synchronized static void coverProps(ProcessNode<?> node){
        if (node.getType().getTypeClass() != Object.class){
            JSONObject props = (JSONObject) node.getProps();
            if (props.get("listeners") instanceof JSONArray){
                //兼容旧版本，旧版本是数组，新版本是对象
                props.put("listeners", "{}");
            }
            node.setProps(props.toJavaObject((Type) node.getType().getTypeClass()));
        }
    }

    /**
     * 判断并加载分支末端所有的节点
     * @param node 当前节点
     */
    private void loadBranchEndNodes(ProcessNode<?> node){
        if (!hasChildren(node) && currentBranchStack.size() > 0){
            //没有后续节点，代表该分支部分结束，塞入末端缓存
            Optional.ofNullable(currentBranchStack.peek()).ifPresent(bn -> {
                List<String> endNodes = footerNode.get(bn.getId());
                if (CollectionUtil.isEmpty(endNodes)){
                    endNodes = new ArrayList<>();
                    footerNode.put(bn.getId(), endNodes);
                }
                if (NodeTypeEnum.EMPTY.equals(node.getType())){
                    currentBranchStack.pop();
                    if (currentBranchStack.size() > 0){
                        Optional.ofNullable(currentBranchStack.peek()).ifPresent(beforeBranch -> {
                            List<String> bfed = footerNode.get(beforeBranch.getId());
                            if (CollectionUtil.isEmpty(bfed)){
                                bfed = new ArrayList<>();
                                footerNode.put(beforeBranch.getId(), bfed);
                            }
                            bfed.add(node.getId());
                        });
                    }
                    currentBranchStack.push(bn);
                }else {
                    endNodes.add(node.getId());
                    //末端如果是审批节点，默认添加一条驳回的链接线
                }
            });
        }
    }

    /**
     * 校验是否存在后续子节点
     * @param node 当前节点
     * @return 1/0
     */
    private boolean hasChildren(ProcessNode<?> node) {
        return ObjectUtil.isNotNull(node.getChildren()) && StrUtil.isNotBlank(node.getChildren().getId());
    }

    /**
     * 递归加载所有流程树
     * @param node 起始节点
     */
    public void loadProcess(ProcessNode<?> node){
        if (Objects.isNull(node) || Objects.isNull(node.getId())){
            return;
        }
        loadBranchEndNodes(node);
        coverProps(node);
        nodeMap.put(node.getId(), node);
        FlowElement element = null;
        switch (node.getType()){
            case ROOT:
                if (isSub){
                    //子流程的发起人节点换成初始化任务节点
                    element = createSubProcInitTask(node.getId());
                }else {
                    element = createInitiatorNode(node.getId(), node.getName());
                }
                break;
            case TASK: //办理和审批都是同一类型操作
            case APPROVAL:
                element = createApprovalNode((ProcessNode<ApprovalProps>) node);
                break;
            case CC:
                element = createCcTask((ProcessNode<CcProps>) node);
                break;
            case DELAY:
                element = createDelayNode((ProcessNode<DelayProps>) node);
                break;
            case SUBPROC:
                element = createSubProcessNode((ProcessNode<SubProcessProps>) node);
                break;
            case INCLUSIVES:
                currentBranchStack.push(node);
                element = createInclusivesNode(node);
                node.getBranchs().forEach(this::loadProcess);
                break;
            case CONDITIONS:
                currentBranchStack.push(node);
                element = createConditionsNode(node);
                node.getBranchs().forEach(this::loadProcess);
                break;
            case CONCURRENTS:
                currentBranchStack.push(node);
                element = createConcurrentNode(node);
                node.getBranchs().forEach(this::loadProcess);
                break;
            case TRIGGER:
                element = createTriggerTask((ProcessNode<TriggerProps>) node);
                break;
            case EMPTY:
                if (NodeTypeEnum.CONCURRENTS.equals(node.getParentType())){
                    //并行网关成对存在，因此再添加一个聚合节点
                    element = createConcurrentNode(node);
                } else if (NodeTypeEnum.CONDITIONS.equals(node.getParentType())) {
                    //构造条件网关合流点
                    element = createConditionsNode(node);
                } else if (NodeTypeEnum.INCLUSIVES.equals(node.getParentType())) {
                    element = createInclusivesNode(node);
                }
                break;
        }
        if (Objects.nonNull(element)){
            addFlowEl(element);
        }
        addAndCreateConnLine(node);
        loadProcess(node.getChildren());
    }

    //构造子流程
    private CallActivity createSubProcessNode(ProcessNode<SubProcessProps> node) {
        CallActivity callActivity = new CallActivity();
        callActivity.setId(node.getId());
        callActivity.setName(node.getName());
        callActivity.setCalledElement(node.getProps().getSubProcCode());
        callActivity.setSameDeployment(Boolean.TRUE.equals(node.getProps().getSyncVersion()));
        callActivity.setBehavior(WflowCallActivityBehavior.class);
        //将主流程内子流程节点id绑定到子流程的业务key
        callActivity.setBusinessKey(node.getId());
        if (node.getProps().getSubAll()){
            callActivity.setInheritVariables(true);
        }else {
            //挨个设置输入变量
            List<IOParameter> ioParams = getIoParams(node.getProps().getInVar(), false);
            callActivity.setInParameters(ioParams);
        }
        //挨个设置输出变量
        callActivity.setOutParameters(getIoParams(node.getProps().getOutVar(), true));
        return callActivity;
    }

    //发起人节点
    private UserTask createInitiatorNode(String id, String name){
        UserTask userTask = new UserTask();
        userTask.setId(id);
        userTask.setName(name);
        userTask.setExecutionListeners(nodeListeners);
        userTask.setAssignee("${" + WflowGlobalVarDef.INITIATOR + "}");
        return userTask;
    }

    //触发器任务
    private FlowElement createTriggerTask(ProcessNode<TriggerProps> node) {
        ServiceTask ccTask = new ServiceTask();
        ccTask.setId(node.getId());
        ccTask.setName(node.getName());
        ccTask.setImplementationType("class");
        ccTask.setImplementation(TriggerServiceTask.class.getName());
        return ccTask;
    }

    //将当前节点向上连接，向上查找应该连接到本节点的所有父节点
    private void addAndCreateConnLine(ProcessNode<?> node){
        if (NodeTypeEnum.EMPTY.equals(node.getType())){
            //空节点代表一个分支结束，缓存出栈
            ProcessNode<?> branch = currentBranchStack.pop();
            footerNode.get(branch.getId()).forEach(en -> {
                SequenceFlow connectLine = createdConnectLine(en, node.getId());
                ProcessNode<?> endNode = nodeMap.get(en);
                if (NodeTypeEnum.CONDITION.equals(endNode.getType())
                    || NodeTypeEnum.INCLUSIVE.equals(endNode.getType())){
                    //分支只有一个条件节点，那么就直连排他网关
                    connectLine.setId(endNode.getId());
                    connectLine.setSourceRef(endNode.getParentId());
                }else if (NodeTypeEnum.EMPTY.equals(endNode.getType())){
                    //末端是网关，要为其构建出口
                    Gateway gateway = (Gateway) elementMap.get(en);
                    gateway.setOutgoingFlows(CollectionUtil.newArrayList(connectLine));
                }
                addFlowEl(connectLine);
            });
            if (NodeTypeEnum.CONDITIONS.equals(node.getParentType())
                    || NodeTypeEnum.INCLUSIVES.equals(node.getParentType())){
                //为条件网关构造出口及默认流
                Gateway cdgw = (Gateway) elementMap.get(node.getParentId());
                List<SequenceFlow> outgoing = branch.getBranchs().stream()
                        .map(v -> {
                            ConditionProps props = (ConditionProps) v.getProps();
                            if (ConditionModeEnum.SIMPLE.equals(props.getMode())
                                    && CollectionUtil.isEmpty(props.getGroups())
                                    && StrUtil.isBlank(cdgw.getDefaultFlow())) {
                                cdgw.setDefaultFlow(v.getId()); //设置默认条件
                            }
                            return (SequenceFlow) elementMap.get(v.getId());
                        })
                        .collect(Collectors.toList());
                cdgw.setOutgoingFlows(outgoing);
            }
        }else if (Objects.nonNull(node.getParentId())){
            //非空节点，特殊处理，判断父级是啥子
            ProcessNode<?> parentNode = nodeMap.get(node.getParentId());
            if (Objects.isNull(parentNode)
                    || NodeTypeEnum.CONDITION.equals(node.getType())
                    || NodeTypeEnum.CONCURRENT.equals(node.getType())
                    || NodeTypeEnum.INCLUSIVE.equals(node.getType())){
                return;
            }
            SequenceFlow line = null;
            switch (parentNode.getType()){
                case CONCURRENT:
                case CONDITION:
                case INCLUSIVE:
                    //并行分支、条件分支、包容分支，构造条件并连接到条件网关
                    line = createdConnectLine(parentNode.getParentId(), node.getId());
                    line.setId(parentNode.getId());
                    line.setName(parentNode.getName());
                    if (parentNode.getProps() instanceof ConditionProps){
                        ConditionProps props = (ConditionProps) parentNode.getProps();
                        if (!ConditionModeEnum.SIMPLE.equals(props.getMode()) || CollectionUtil.isNotEmpty(props.getGroups())){
                            String conditionExplainCreator = conditionExplainCreator(line.getId());
                            if (StrUtil.isNotBlank(conditionExplainCreator)){
                                line.setConditionExpression(conditionExplainCreator);
                            }
                        }
                    }
                    break;
                case APPROVAL:
                case TASK:
                    //父级节点是审批/办理节点
                    line = createdConnectLine(parentNode.getId(), node.getId());
                    line.setName(NodeTypeEnum.APPROVAL.equals(parentNode.getType()) ? "同意": "提交");
                    break;
                case EMPTY:
                    line = createdConnectLine(parentNode.getId(), node.getId());
                    //为合流点构造出口流
                    Gateway gateway2 = (Gateway) elementMap.get(node.getParentId());
                    gateway2.setOutgoingFlows(CollectionUtil.newArrayList(line));
                    break;
                default:
                    line = createdConnectLine(parentNode.getId(), node.getId());
                    break;
            }
            addFlowEl(line);
        }else if (NodeTypeEnum.ROOT.equals(node.getType())){
            //发起人节点链接到开始节点
            addFlowEl(createdConnectLine("start", node.getId()));
        }
    }

    private void addFlowEl(FlowElement element){
        elementMap.put(element.getId(), element);
    }

    //审批-用户任务
    private UserTask createApprovalNode(ProcessNode<ApprovalProps> node) {
        UserTask userTask = new UserTask();
        userTask.setName(node.getName());
        ApprovalProps props = node.getProps();
        //全部按多人审批处理
        userTask.setExecutionListeners(nodeListeners);
        userTask.setTaskListeners(taskListeners);
        if(ApprovalTypeEnum.SELF.equals(props.getAssignedType())){
            //发起人自己审批
            userTask.setAssignee("${" + WflowGlobalVarDef.INITIATOR + "}");
        }else {
            userTask.setAssignee("${assignee}");
            userTask.setLoopCharacteristics(createAndOrMode(node.getId(), props));
        }
        userTask.setId(node.getId());
        //处理审批超时，添加定时器边界事件
        ApprovalProps.TimeLimit timeLimit = props.getTimeLimit();
        if (Objects.nonNull(timeLimit.getTimeout().getValue()) && timeLimit.getTimeout().getValue() > 0){
            BoundaryEvent boundaryEvent = new BoundaryEvent();
            boundaryEvent.setId(node.getId() + "-timeout");
            boundaryEvent.setName("审批超时");
            TimerEventDefinition timerEventDefinition = new TimerEventDefinition();
            String timeValue = getISO8601Time(timeLimit.getTimeout().getValue(), timeLimit.getTimeout().getUnit());
            timerEventDefinition.setTimeCycle("R/" + timeValue); //默认无限循环
            if (ApprovalProps.TimeLimit.Handler.HandlerType.NOTIFY.equals(timeLimit.getHandler().getType())){
                //根据是否循环提醒来修改定时规则
                ApprovalProps.TimeLimit.Handler.Notify notify = timeLimit.getHandler().getNotify();
                if (!notify.isOnce()){
                    //默认最大10次，这里参考 https://en.wikipedia.org/wiki/ISO_8601#Repeating_intervals
                    timerEventDefinition.setTimeCycle("R10/" + timeValue);
                }else {
                    timerEventDefinition.setTimeCycle(null);
                    timerEventDefinition.setTimeDuration(timeValue);
                }
            }
            boundaryEvent.addEventDefinition(timerEventDefinition);
            boundaryEvent.setCancelActivity(false);
            boundaryEvent.setAttachedToRef(userTask);
            //创建边界事件的出口
            ServiceTask timeoutTask = new ServiceTask();
            timeoutTask.setId(node.getId() + "-timeoutTask");
            timeoutTask.setName(node.getName() + "超时处理");
            timeoutTask.setImplementationType("class");
            timeoutTask.setImplementation(ApprovalTimeoutServiceTask.class.getName());
            addFlowEl(boundaryEvent);
            addFlowEl(timeoutTask);
            addFlowEl(createdConnectLine(boundaryEvent.getId(), timeoutTask.getId()));
        }
        return userTask;
    }

    //并行块节点-并行网关
    private ParallelGateway createConcurrentNode(ProcessNode<?> node) {
        ParallelGateway parallelGateway = new ParallelGateway();
        parallelGateway.setId(node.getId());
        parallelGateway.setName(NodeTypeEnum.EMPTY.equals(node.getType()) ? "并行分支聚合":"并行分支");
        return parallelGateway;
    }

    //抄送任务
    private ServiceTask createCcTask(ProcessNode<CcProps> node) {
        ServiceTask ccTask = new ServiceTask();
        ccTask.setId(node.getId());
        ccTask.setName(node.getName());
        ccTask.setImplementationType("class");
        ccTask.setImplementation(CcServiceTask.class.getName());
        //ccTask.addAttribute();
        return ccTask;
    }

    //子流程初始化任务
    private ServiceTask createSubProcInitTask(String id) {
        ServiceTask initTask = new ServiceTask();
        initTask.setId(id);
        initTask.setName("子流程初始化");
        initTask.setImplementationType("class");
        initTask.setImplementation(SubProcessInitTask.class.getName());
        return initTask;
    }

    //构建延时节点
    private IntermediateCatchEvent createDelayNode(ProcessNode<DelayProps> node) {
        IntermediateCatchEvent catchEvent = new IntermediateCatchEvent();
        TimerEventDefinition timerDefinition = new TimerEventDefinition();
        DelayProps props = node.getProps();
        if (props.getType().equals(DelayProps.Type.FIXED)) {
            timerDefinition.setTimeDuration(getISO8601Time(props.getTime(), props.getUnit()));
        } else {
            //动态计算时长
            timerDefinition.setTimeDate("${uelTools.getDelayDuration(execution)}");
        }
        catchEvent.setId(node.getId());
        catchEvent.setName(node.getName());
        //插入定时器捕获中间事件
        catchEvent.addEventDefinition(timerDefinition);
        return catchEvent;
    }

    //条件块节点-排他网关
    private ExclusiveGateway createConditionsNode(ProcessNode<?> node) {
        ExclusiveGateway exclusiveGateway = new ExclusiveGateway();
        exclusiveGateway.setExclusive(true);
        exclusiveGateway.setId(node.getId());
        exclusiveGateway.setName(NodeTypeEnum.EMPTY.equals(node.getType()) ? "条件分支聚合":"条件分支");
        return exclusiveGateway;
    }

    //包容网关
    private InclusiveGateway createInclusivesNode(ProcessNode<?> node) {
        InclusiveGateway inclusiveGateway = new InclusiveGateway();
        inclusiveGateway.setExclusive(true);
        inclusiveGateway.setId(node.getId());
        inclusiveGateway.setName(NodeTypeEnum.EMPTY.equals(node.getType()) ? "包容分支聚合":"包容分支");
        return inclusiveGateway;
    }

    private SequenceFlow createdConnectLine(String source, String target) {
        SequenceFlow flow = new SequenceFlow();
        flow.setId(source + "_" + target);
        flow.setSourceRef(source);
        flow.setTargetRef(target);
        return flow;
    }

    //构建条件表达式
    private String conditionExplainCreator(String nodeId) {
        return "${uelTools.conditionCompare('"+ nodeId + "', execution)}";
    }

    //多人签署设置-会签/或签
    private MultiInstanceLoopCharacteristics createAndOrMode(String nodeId, ApprovalProps props) {
        MultiInstanceLoopCharacteristics loopCharacteristics = new MultiInstanceLoopCharacteristics();
        loopCharacteristics.setId(IdUtil.randomUUID());
        loopCharacteristics.setElementVariable("assignee");
        loopCharacteristics.setInputDataItem("${processTaskService.getNodeApprovalUsers(execution)}");
        //设置完成条件，先判断会签还是或签
        String completionCondition = "";
        switch (props.getMode()) {
            case OR: //有任意一个人处理过就结束
                completionCondition = "nrOfCompletedInstances >= 1";
                loopCharacteristics.setSequential(false);
                break;
            case AND: //所有任务都结束
                completionCondition = "nrOfInstances == nrOfCompletedInstances";
                loopCharacteristics.setSequential(false);
                break;
            case NEXT:
                completionCondition = "nrOfInstances == nrOfCompletedInstances";
                loopCharacteristics.setSequential(true);
                break;
        }
        loopCharacteristics.setCompletionCondition("${" + completionCondition + "}");
        return loopCharacteristics;
    }

    /**
     * 获取ISO8601时间
     * @param time 值
     * @param unit 单位
     * @return 格式化时间
     */
    private String getISO8601Time(Integer time, String unit){
        switch (unit){
            case "D": return "P" + time + unit;
            case "H": return "PT" + time + unit;
            case "M": return "PT" + time + unit;
        }
        return null;
    }

    /**
     * 构造主子流程变量传递参数
     * @param vars 参数
     * @param reverse 是否为逆向传递
     * @return 参数列表
     */
    private List<IOParameter> getIoParams(List<SubProcessProps.Var> vars, Boolean reverse){
        return vars.stream().filter(v -> StrUtil.isNotBlank(reverse ? v.getSKey() : v.getMKey()))
                .map(v -> {
                    IOParameter ip = new IOParameter();
                    if (reverse){
                        ip.setSource(v.getSKey());
                        ip.setTarget(StrUtil.isNotBlank(v.getMKey()) ? v.getMKey() : v.getSKey());
                    }else {
                        ip.setSource(v.getMKey());
                        ip.setTarget(StrUtil.isNotBlank(v.getSKey()) ? v.getSKey() : v.getMKey());
                    }
                    return ip;
                })
                .collect(Collectors.toList());
    }
}

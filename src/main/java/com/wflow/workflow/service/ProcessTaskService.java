package com.wflow.workflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wflow.workflow.bean.process.NodeProps;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.bean.vo.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author : JoinFyc
 * @date : 2024/8/25
 */
public interface ProcessTaskService {

    /**
     * 获取待办
     *
     * @param pageSize 每页条数
     * @param pageNo   页码
     * @param code     表单编号
     * @return 待办任务列表
     */
    Page<ProcessTaskVo> getUserTodoList(Integer pageSize, Integer pageNo, String code,
                                        String[] startTimes, String keyword);

    /**
     * 获取已办
     *
     * @param pageSize 每页条数
     * @param pageNo   页码
     * @param code     表单编号
     * @return 已办任务列表
     */
    Page<ProcessTaskVo> getUserIdoList(Integer pageSize, Integer pageNo, String code);

    /**
     * 获取节点审批人，被flowable uel表达式自动调用
     *
     * @param execution 执行实例
     * @return 该任务的审批人
     */
    List<String> getNodeApprovalUsers(ExecutionEntity execution);

    /**
     * 处理任务
     * @param taskUser 任务原操作人
     * @param operationUser 当前操作人
     * @param params 任务参数
     */
    void approvalTask(String taskUser, String operationUser, ProcessHandlerParamsVo params);

    /**
     * 获取抄送用户ID集合
     *
     * @param instanceId 审批实例ID
     * @param nodeId     节点ID
     * @return 被抄送的用户
     */
    Set<String> getCcTaskUsers(String instanceId, String nodeId);

    /**
     * 下版实现
     * 获取等待中且还未开始的任务，如果存在条件则需要直接解析条件
     *
     * @param instance  实例
     * @param startDept 发起部门
     * @param nodeMap   流程节点Map
     * @return 未开始的任务
     */
    List<ProcessProgressVo.ProgressNode> getFutureTask(HistoricProcessInstance instance, String startDept, Map<String, Object> vars, Map<String, ProcessNode<?>> nodeMap);

    /**
     * 获取指定任务所有可回退的审批任务节点
     *
     * @param taskId     当前任务ID
     * @param instanceId 审批实例ID
     * @return 所有可回退节点
     */
    List<HisApprovalNodeVo> getRecallTaskNodes(String instanceId, String taskId);

    /**
     * 获取节点审批设置项
     *
     * @param taskId 要处理的任务ID
     * @return 该任务所属节点的设置项
     */
    NodeSettingsVo getNodeTaskSettings(String taskId);

    /**
     * 工作交接接口
     * @param sourceUser 离职人
     * @param targetUser 交接人
     */
    void workHandover(String sourceUser, String targetUser);

    List<String> getApprovalUsers(String instanceId, String nodeId, ApprovalProps props);
}

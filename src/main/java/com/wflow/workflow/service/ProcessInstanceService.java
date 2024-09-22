package com.wflow.workflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wflow.workflow.bean.vo.*;
import org.flowable.task.api.Task;

import java.util.List;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/8/22
 */
public interface ProcessInstanceService {

    String startProcess(String defId, ProcessStartParamsVo params);

    String getBeforeTask(String instanceId, String task);

    void delProcessInstance(String instanceId);

    ProcessProgressVo getInstanceProgress(String nodeId, String instanceId);

    List<Task> getProcessInstanceTaskList(String instanceId);

    /**
     * 获取用户发起的流程实例
     * @param pageSize 每页数量
     * @param pageNo 页码
     * @param startUser  发起人
     * @param code 表单流程ID（流程定义KEY）
     * @param finished 流程是否已经结束
     * @param startTimes 发起流程的时间范围
     * @param keyword 关键字（流程实例名、发起人名）
     * @param fieldId 字段ID
     * @param fieldVal 字段值
     * @return 列表数据
     */
    Page<ProcessInstanceVo> getUserSubmittedList(Integer pageSize, Integer pageNo, String startUser, String code,
                                                 Boolean finished, String[] startTimes, String keyword,
                                                 String fieldId, String fieldVal);

    /**
     * 获取系统抄送我的流程
     *
     * @param pageSize  每页数量
     * @param pageNo    页码
     * @param code      表单流程ID（流程定义KEY）
     * @param startTimes 抄送开始时间范围
     * @return 列表数据
     */
    Page<ProcessInstanceVo> getCcMeInstance(Integer pageSize, Integer pageNo, String code, String[] startTimes);

    InstanceCountVo getProcessInstanceCount();
}

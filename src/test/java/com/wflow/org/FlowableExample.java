package com.wflow.org;

import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

/**
 * @author JoinFyc
 * @description 流程引擎-示例Example
 * @date 2024-08-02
 */
public class FlowableExample {

    /**
     * 主要用于管理流程定义，例如部署流程、查询流程定义、删除流程定义
     */
    private RepositoryService repositoryService;

    /**
     * 主要用于管理流程实例和执行，例如启动流程实例、获取流程实例状态、终止流程实例
     */
    private RuntimeService runtimeService;

    /**
     * 主要用于管理Flowable引擎的元数据和统计信息，例如获取引擎的数据库表信息、监控引擎状态等
     */
    private ManagementService managementService;

    /**
     * 主要用于访问历史数据，例如查询已完成的流程实例、历史任务、历史活动
     */
    private HistoryService historyService;

    /**
     * 主要用于管理任务，例如查询任务、完成任务、委派任务
     */
    private TaskService taskService;

    public void example() {
        // 部署流程
        repositoryService.createDeployment().addClasspathResource("processes/my-process.bpmn").deploy();

        // 启动流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcessKey");

        // 查询任务
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // 完成任务
        taskService.complete(task.getId());

        // 查询历史
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

        // 打印历史信息
        System.out.println("Historic Process Instance: " + historicProcessInstance.getProcessDefinitionId());

        // 获取数据库表信息
        TableMetaData tableMetaData = managementService.getTableMetaData("ACT_RU_TASK");
        System.out.println("Table MetaData: " + tableMetaData);
    }

}
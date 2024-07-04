package com.wflow.org;

import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 * @author JoinFyc
 * @description
 * @date 2024-06-27
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProcessEngineTest {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RuntimeService runtimeService;


    @Autowired
    private ManagementService managementService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Test
    public void deployProcessDefinitionTest() {

        /**
         * Flowable -- 核心组件
         * 流程引擎（Process Engine）：处理 BPMN 2.0 流程定义的执行。
         * 任务服务（Task Service）：管理用户任务。
         * 表单引擎（Form Engine）：处理动态表单。
         * 规则引擎（Rules Engine）：用于业务规则管理。
         * <P></P>
         *
         * 创建一个简单的工作流，以下是一个基本步骤:
         * 1. 定义业务流程：使用 Flowable Modeler 设计 BPMN 流程。
         * 2. 部署流程定义：将 BPMN 流程部署到 Flowable 引擎中。
         * 3. 启动流程实例：在你的应用程序中启动一个流程实例。
         * 4. 任务处理：处理和管理流程中的用户任务。
         */

        // 部署流程定义
        Deployment deployment = repositoryService
                .createDeployment()
                //将Bpmn流程部署到 Flowable 引擎中
                .addClasspathResource("processes/simple-process.bpmn20.xml")
                .deploy();

        // 验证部署
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        System.out.println("Found process definition : " + processDefinition.getName());

        // 启动流程实例
        ProcessInstance processInstance = runtimeService.
                startProcessInstanceByKey("simpleProcess");

        System.out.println("Started process instance id " + processInstance.getId());

    }

    public static void main(String[] args) {

        // 创建流程引擎
        final ProcessEngine processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .buildProcessEngine();

        // 获取服务
        RuntimeService runtimeService = processEngine.getRuntimeService();

        // 部署流程定义
        Deployment deployment = processEngine.getRepositoryService()
                .createDeployment()
                //将Bpmn流程部署到 Flowable 引擎中
                .addClasspathResource("processes/simple-process.bpmn20.xml")
                .deploy();

        // 验证部署
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
                .createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        System.out.println("Found process definition : " + processDefinition.getName());

        // 启动流程实例
        ProcessInstance processInstance = runtimeService.
                startProcessInstanceByKey("simpleProcess");

        System.out.println("Started process instance id " + processInstance.getId());

    }

}

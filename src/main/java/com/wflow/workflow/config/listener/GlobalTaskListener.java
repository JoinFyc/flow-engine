package com.wflow.workflow.config.listener;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.wflow.workflow.bean.dto.NotifyDto;
import com.wflow.workflow.bean.process.ProcessStatus;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.execute.ListenerExecutor;
import com.wflow.workflow.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.event.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * @author : willian fu
 * @date : 2022/8/27
 */
@Slf4j
@Component
public class GlobalTaskListener extends AbstractFlowableEngineEventListener implements CommandLineRunner {

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ListenerExecutor listenerExecutor;

    @Override
    public void run(String... args) {
        //把当前类注册到全局事件监听
        runtimeService.addEventListener(this);
    }

    @Override
    protected void taskCreated(FlowableEngineEntityEvent event) {
        log.debug("监听到任务[{}]创建", event.getExecutionId());
        super.taskCreated(event);
    }

    @Override
    protected void processCreated(FlowableEngineEntityEvent event) {
        listenerExecutor.doProcessChangeHandler("start", event.getProcessInstanceId(), event.getProcessDefinitionId());
        //流程创建成功
        super.processCreated(event);
    }

    @Override
    protected void activityStarted(FlowableActivityEvent event) {
        log.info("流程[{}]进入ID[{}]的[{}]节点", event.getProcessInstanceId(),  event.getActivityId(), event.getActivityName());
        listenerExecutor.doProcessNodeChangeHandler("enter", event.getProcessInstanceId(),
                event.getProcessDefinitionId(), event.getActivityId(), event.getActivityType());
        super.activityStarted(event);
    }

    @Override
    protected void activityCompleted(FlowableActivityEvent event) {
        processLeaveNodeEventHandler(event);
        super.activityCompleted(event);
    }

    @Override
    protected void multiInstanceActivityCompletedWithCondition(FlowableMultiInstanceActivityCompletedEvent event) {
        processLeaveNodeEventHandler(event);
        super.multiInstanceActivityCompletedWithCondition(event);
    }

    @Override
    protected void taskCompleted(FlowableEngineEntityEvent event) {
        log.debug("监听到任务[{}]结束", event.getExecutionId());
        super.taskCompleted(event);
    }

    @Override
    protected void multiInstanceActivityCancelled(FlowableMultiInstanceActivityCancelledEvent event) {
        //TODO 节点取消事件，退回和跳转涉及的节点需要触发的话可以解除本注释
        //processLeaveNodeEventHandler(event);
        super.multiInstanceActivityCancelled(event);
    }

    @Override
    protected void activityCancelled(FlowableActivityCancelledEvent event) {
        //TODO 节点取消事件，退回和跳转涉及的节点需要触发的话可以解除本注释
        //processLeaveNodeEventHandler(event);
        super.activityCancelled(event);
    }

    @Override
    protected void processCompleted(FlowableEngineEntityEvent event) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(event.getProcessInstanceId())
                .singleResult();
        //清除该流程缓存
        clearProcessVars(event.getProcessInstanceId());
        notifyService.notify(NotifyDto.builder()
                .target(instance.getStartUserId())
                .instanceId(event.getProcessInstanceId())
                .processDefId(event.getProcessDefinitionId())
                .title("您的审批已通过")
                .type(NotifyDto.TypeEnum.SUCCESS)
                .content(StrUtil.builder("您提交的审批【",
                        instance.getProcessDefinitionName(), "】已经通过").toString())
                .build());
        runtimeService.updateBusinessStatus(event.getProcessInstanceId(), ProcessStatus.PASS.toString());
        listenerExecutor.doProcessChangeHandler("pass", event.getProcessInstanceId(), event.getProcessDefinitionId());
        log.info("[{}]审批流程[{}}]通过", instance.getProcessInstanceId(), instance.getProcessDefinitionName());
        super.processCompleted(event);
    }

    @Override
    protected void processCompletedWithTerminateEnd(FlowableEngineEntityEvent event) {
        clearProcessVars(event.getProcessInstanceId());
        //通过判断流程实例的endActivityId = refuse-end / cancel-end 判断是撤销还是驳回
        if (event instanceof FlowableProcessTerminatedEvent){
            Object cause = ((FlowableProcessTerminatedEvent) event).getCause();
            if (cause instanceof EndEvent){
                String endNode = ((EndEvent) cause).getId();
                if ("refuse-end".equals(endNode)){
                    runtimeService.updateBusinessStatus(event.getProcessInstanceId(), ProcessStatus.REFUSE.toString());
                    log.debug("监听到流程[{}]被驳回", event.getProcessInstanceId());
                    listenerExecutor.doProcessChangeHandler("refuse", event.getProcessInstanceId(), event.getProcessDefinitionId());
                } else if ("cancel-end".equals(endNode)) {
                    runtimeService.updateBusinessStatus(event.getProcessInstanceId(), ProcessStatus.CANCEL.toString());
                    listenerExecutor.doProcessChangeHandler("cancel", event.getProcessInstanceId(), event.getProcessDefinitionId());
                    log.debug("监听到流程[{}]被撤销", event.getProcessInstanceId());
                }
            }
        }
        super.processCompletedWithTerminateEnd(event);
    }

    private void processLeaveNodeEventHandler(FlowableActivityEvent event){
        log.info("流程[{}]离开ID[{}]的[{}]节点", event.getProcessInstanceId(), event.getActivityId(), event.getActivityName());
        listenerExecutor.doProcessNodeChangeHandler("leave", event.getProcessInstanceId(),
                event.getProcessDefinitionId(), event.getActivityId(), event.getActivityType());
    }

    //流程结束，对变量及缓存进行清理
    private void clearProcessVars(String instanceId){
        //清除该流程缓存
        UserTaskListener.TASK_AGREES.remove(instanceId);
    }
}

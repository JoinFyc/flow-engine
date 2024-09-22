package com.wflow.workflow.config.custom;

import cn.hutool.core.util.IdUtil;
import org.flowable.engine.impl.bpmn.deployer.BpmnDeployer;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;

/**
 * 重写流程定义id生成规则, 生成规则为：wf+uuid，之前的规则为：流程定义key:版本号:+id，太长了
 * @author : JoinFyc
 * @date : 2024/2/23
 */
public class CustomIdBpmnDeployer extends BpmnDeployer {

    protected String getIdForNewProcessDefinition(ProcessDefinitionEntity processDefinition){
        return "wf" + IdUtil.simpleUUID();
    }
}

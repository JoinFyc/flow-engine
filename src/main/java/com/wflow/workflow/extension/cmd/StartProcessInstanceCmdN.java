package com.wflow.workflow.extension.cmd;

import com.wflow.workflow.bean.process.ProcessStatus;
import org.flowable.engine.impl.cmd.StartProcessInstanceCmd;

import java.util.Map;

/**
 * 自定义流程启动命令，设置流程实例名称
 * @author : willian fu
 * @date : 2024/3/11
 */
public class StartProcessInstanceCmdN<T> extends StartProcessInstanceCmd<T> {
    public StartProcessInstanceCmdN(String processInstanceName, String processDefinitionId, String businessKey, Map<String, Object> variables, String tenantId) {
        super(null, processDefinitionId, businessKey, variables, tenantId);
        this.processInstanceName = processInstanceName;
        this.businessStatus = ProcessStatus.RUNNING.toString();
    }
}

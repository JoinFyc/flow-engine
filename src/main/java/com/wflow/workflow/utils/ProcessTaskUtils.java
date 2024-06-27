package com.wflow.workflow.utils;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.service.UserDeptOrLeaderService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2022/8/28
 */
@Deprecated
@Component
public class ProcessTaskUtils{

    @Autowired
    private RepositoryService repositoryService;

    public List<FlowElement> getNextTask(Task task){
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process process = bpmnModel.getMainProcess();
        process.getFlowElements().forEach(el -> {
            //el.
        });
        return null;
    }

    /**
     * 执行UEL表达式
     * @param var 变量
     * @param expression 表达式
     * @return 解析结果
     */
    public static boolean expressionResult(Map<String, Object> var, String expression) {
        Expression exp = AviatorEvaluator.compile(expression);
        final Object execute = exp.execute(var);
        return Boolean.parseBoolean(String.valueOf(execute));
    }
}

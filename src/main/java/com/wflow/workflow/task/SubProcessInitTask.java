package com.wflow.workflow.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.mapper.WflowSubProcessMapper;
import com.wflow.utils.SpringContextUtil;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.ProcessStatus;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.config.callActivity.WflowCallActivityBehavior;
import com.wflow.workflow.service.ProcessNodeCacheService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : JoinFyc
 * @date : 2023/12/12
 * 子流程初始化任务，用来初始化子流程变量等信息，会在子流程启动后调用
 */
@Slf4j
public class SubProcessInitTask implements JavaDelegate {

    public static WflowSubProcessMapper processMapper;
    public static ProcessNodeCacheService nodeCacheService;

    public static RuntimeService runtimeService;

    public SubProcessInitTask() {
        runtimeService = SpringContextUtil.getBean(RuntimeService.class);
        processMapper = SpringContextUtil.getBean(WflowSubProcessMapper.class);
        nodeCacheService = SpringContextUtil.getBean(ProcessNodeCacheService.class);
    }

    @Override
    public void execute(DelegateExecution execution) {
        WflowSubProcess subProcess = processMapper.selectOne(new LambdaQueryWrapper<WflowSubProcess>()
                .select(WflowSubProcess::getProcess)
                .eq(WflowSubProcess::getProcDefId, execution.getProcessDefinitionId()));
        Map<String, ProcessNode<?>> nodeMap = nodeCacheService.reloadProcessByStr(subProcess.getProcess());
        Map<String, Object> propsMap = nodeMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                v -> null == v.getValue().getProps() ? new HashMap<>() : v.getValue().getProps()));
        ProcessInstanceOwnerDto ownerDto = WflowCallActivityBehavior.OWNER.get();
        execution.setVariable(WflowGlobalVarDef.INITIATOR, ownerDto.getOwner());
        execution.setVariable(WflowGlobalVarDef.START_DEPT, ownerDto.getOwnerDeptId());
        //设置节点流程变量缓存
        execution.setVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS, propsMap);
        runtimeService.updateBusinessStatus(execution.getProcessInstanceId(), ProcessStatus.RUNNING.toString());
        log.info("设置子流程{}流程变量[{}, {}]", execution.getProcessInstanceId(), ownerDto.getOwner(), ownerDto.getOwnerDeptId());
    }
}

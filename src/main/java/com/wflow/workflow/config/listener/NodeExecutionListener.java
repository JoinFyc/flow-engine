package com.wflow.workflow.config.listener;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.workflow.config.WflowGlobalVarDef;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;

/**
 * @description : 节点执行实例监听器
 * 场景：
 *     流程启动和结束：可以用来在流程启动时初始化数据，或者在流程结束时进行清理。
 *     节点流转：可以用来处理流程在节点之间流转时的逻辑。
 *     流程级别事件：处理更高层次的流程事件，比如流程实例创建、完成、取消等。
 * @author : JoinFyc
 * @date : 2024/8/07
 */
@Slf4j
@Component("nodeExecutionListener")
public class NodeExecutionListener implements ExecutionListener {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private WflowModelHistorysMapper historysMapper;

    @Override
    public void notify(DelegateExecution execution) {
        log.info("NodeExecutionListener,{},{}",execution.getId(),execution.getEventName());
        if (execution.getParentId().equals(execution.getProcessInstanceId())){
            //节点结束前执行一些操作
            FlowElement element = execution.getCurrentFlowElement();
            //节点完成前执行操作，必须在这里进行拦截
            taskNodeCompleteHandler(execution.getProcessInstanceId(), execution.getProcessDefinitionId(), element.getId());
        }
    }

    /**
     * 处理节点离开事件，主要处理回退后再继续，是否直达回退前的节点
     * @param instanceId 流程实例ID
     * @param defId 流程定义ID
     * @param nodeId 节点ID
     */
    public void taskNodeCompleteHandler(String instanceId, String defId, String nodeId){
        //判断下是不是回退后执行的操作，先拿之前执行回退的那个节点的ID
        Object bfNode = runtimeService.getVariable(instanceId, WflowGlobalVarDef.NODE_RETURN);
        if (Objects.nonNull(bfNode)){
            WflowModelHistorys model = historysMapper.selectOne(new LambdaQueryWrapper<WflowModelHistorys>()
                    .select(WflowModelHistorys::getSettings).eq(WflowModelHistorys::getProcessDefId, defId));
            JSONObject settings = JSONObject.parseObject(model.getSettings());
            //取回退设置
            Boolean reExecute = settings.getBoolean("reExecute");
            if (Boolean.FALSE.equals(reExecute)){
                //存在回退操作，并行俩节点能连接到，再执行回退，考虑并行单分支发生回退情况
                BpmnModel bpmnModel = repositoryService.getBpmnModel(defId);
                FlowNode sflowNode = (FlowNode) bpmnModel.getFlowElement(nodeId);
                FlowNode tflowNode = (FlowNode) bpmnModel.getFlowElement(bfNode.toString());
                if (ExecutionGraphUtil.isReachable(bpmnModel.getMainProcess(), sflowNode, tflowNode, new HashSet<>())){
                    //删除回退标记
                    runtimeService.removeVariable(instanceId, WflowGlobalVarDef.NODE_RETURN);
                    //执行流程跳转
                    log.info("流程[{}]回退后开始跳转[{}]继续", instanceId, tflowNode.getName());
                    runtimeService.createChangeActivityStateBuilder().processInstanceId(instanceId)
                            .moveActivityIdTo(nodeId, bfNode.toString()).changeState();
                }
            }
        }
    }
}

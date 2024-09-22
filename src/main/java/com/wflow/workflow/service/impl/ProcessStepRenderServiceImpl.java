package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.exception.BusinessException;
import com.wflow.utils.WfCatchUtil;
import com.wflow.workflow.UELTools;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.enums.ConditionModeEnum;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.bean.process.props.ConditionProps;
import com.wflow.workflow.bean.vo.ProcessConditionResolveParamsVo;
import com.wflow.workflow.execute.ElExecute;
import com.wflow.workflow.service.ProcessModelService;
import com.wflow.workflow.service.ProcessNodeCacheService;
import com.wflow.workflow.service.ProcessStepRenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author : JoinFyc
 * @date : 2023/11/10
 */
@Slf4j
@Service
public class ProcessStepRenderServiceImpl implements ProcessStepRenderService {

    @Autowired
    private UELTools uelTools;

    @Autowired
    private ProcessModelService modelService;

    @Autowired
    private ProcessNodeCacheService nodeCatchService;

    @Override
    public List<String> getIsTrueConditions(ProcessConditionResolveParamsVo paramsVo) {
        WflowModelHistorys model = modelService.getModelByDefId(paramsVo.getProcessDfId());
        if (Objects.isNull(model)){
            throw new BusinessException("未找到该流程版本记录");
        }
        ProcessNode<?> node = WfCatchUtil.getCatch(paramsVo.getProcessDfId() + paramsVo.getConditionNodeId(), ProcessNode.class);
        if (Objects.isNull(node)){
            Map<String, ProcessNode<?>> nodeMap = nodeCatchService.getProcessNode(paramsVo.getProcessDfId());
            node = nodeMap.get(paramsVo.getConditionNodeId());
            //加载到缓存,2 分钟，为了后面的流程能够快速存取解析
            nodeMap.forEach((k, v) -> {
                if (NodeTypeEnum.CONDITIONS.equals(v.getType()) || NodeTypeEnum.INCLUSIVES.equals(v.getType())){
                    v.getBranchs().forEach(bc -> bc.setChildren(null));
                    WfCatchUtil.putCatch(paramsVo.getProcessDfId() + v.getId(), v, 2 * 60000);
                }
            });
        }
        List<String> results = new ArrayList<>(node.getBranchs().size());
        String defaultCondition = null;
        for (ProcessNode<?> branch : node.getBranchs()) {
            ConditionProps props = (ConditionProps) branch.getProps();
            if (Objects.isNull(defaultCondition)
                    && ConditionModeEnum.SIMPLE.equals(props.getMode())
                    && CollectionUtil.isEmpty(props.getGroups())){
                //默认条件
                defaultCondition = branch.getId();
                continue;
            }
            Boolean compare = false;
            try {
                compare = uelTools.conditionCompare(props, paramsVo.getContext());
            } catch (Exception e) {
                log.warn("解析流程定义[ID = {}]的条件网关节点[{}]失败：[{}]", paramsVo.getProcessDfId(), paramsVo.getConditionNodeId(), e.getMessage());
            }
            if (compare) {
                results.add(branch.getId());
                if (!paramsVo.getMultiple()) {
                    break;
                }
            }
        }
        if (CollectionUtil.isEmpty(results)){
            //没有满足的条件，返回默认条件
            results.add(defaultCondition);
        }
        return results;
    }

    @Override
    public Boolean validateEl(String el) {
        try {
            ElExecute.validate(el);
        } catch (Exception e) {
            throw new BusinessException("EL表达式校验失败：" + e.getMessage());
        }
        return true;
    }
}

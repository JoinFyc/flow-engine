package com.wflow.workflow.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.mapper.WflowModelsMapper;
import com.wflow.mapper.WflowSubProcessMapper;
import com.wflow.workflow.WFlowToBpmnCreator;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import com.wflow.workflow.service.ProcessNodeCatchService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

/**
 * 内存实现流程节点缓存服务
 *
 * @author : willian fu
 * @date : 2022/8/24
 */
@Slf4j
@Service
public class MemoryProcessNodeCatchServiceImpl implements ProcessNodeCatchService {

    @Autowired
    private WflowModelsMapper modelsMapper;

    @Autowired
    private WflowModelHistorysMapper historysMapper;

    @Autowired
    private WflowSubProcessMapper subProcessMapper;

    //缓存流程定义ID -> (节点ID -> 节点) 快速取数据，缓存12个小时
    public static final TimedCache<String, Map<String, ProcessNode<?>>> processModelNodeMap = new TimedCache<>(12 * 3600000);
    static {
        //每小时清理一次缓存
        processModelNodeMap.schedulePrune(3600000);
    }

    @Override
    public ProcessNode<?> getProcessNodeById(String code, String nodeId) {
        Map<String, ProcessNode<?>> nodeMap = processModelNodeMap.get(code);
        if (null != nodeMap) {
            return nodeMap.get(nodeId);
        }
        return null;
    }

    @Override
    public ProcessNode<?> getProcessNode(String defId, String nodeId) {
        Map<String, ProcessNode<?>> nodeMap = getProcessNode(defId);
        return nodeMap.get(nodeId);
    }

    @Override
    public Map<String, ProcessNode<?>> getProcessNode(String defId) {
        Map<String, ProcessNode<?>> nodeMap = processModelNodeMap.get(defId);
        if (CollectionUtil.isEmpty(nodeMap)) {
            WflowModelHistorys modelHistorys = historysMapper.selectOne(new LambdaQueryWrapper<WflowModelHistorys>()
                    .select(WflowModelHistorys::getProcess)
                    .eq(WflowModelHistorys::getProcessDefId, defId));
            if (Objects.isNull(modelHistorys)){
                //主流程为空，去子流程找
                WflowSubProcess subProcess = subProcessMapper.selectOne(new LambdaQueryWrapper<WflowSubProcess>()
                        .select(WflowSubProcess::getProcess)
                        .eq(WflowSubProcess::getProcDefId, defId));
                nodeMap = reloadProcessByStr(subProcess.getProcess());
            } else {
                nodeMap = reloadProcessByStr(modelHistorys.getProcess());
            }
            processModelNodeMap.put(defId, nodeMap);
        }
        return nodeMap;
    }

    @Override
    public <T> ProcessNode<T> getProcessNodeById(Class<T> clazz, String code, String nodeId) {
        ProcessNode<?> node = getProcessNodeById(code, nodeId);
        if (null != node) {
            return (ProcessNode<T>) node;
        }
        return null;
    }

    @Override
    public List<ProcessNode<?>> getTaskNodesByCode(String code) {
        try {
            return processModelNodeMap.get(code).values().stream().filter(n ->
                    NodeTypeEnum.ROOT.equals(n.getType())
                            || NodeTypeEnum.APPROVAL.equals(n.getType())
                            || NodeTypeEnum.CC.equals(n.getType())).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public ProcessNode<?> findSubNodeByIdFromRoot(String nodeId, ProcessNode<?> node) {
        //TODO 此处应当利用LRU缓存实现高效取，日后再修改
        if (null != node && null != node.getId()){
            if (nodeId.equals(node.getId())){
                WFlowToBpmnCreator.coverProps(node);
                return node;
            }else if (NodeTypeEnum.CONCURRENTS.equals(node.getType())
                    || NodeTypeEnum.CONDITIONS.equals(node.getType())
                    || NodeTypeEnum.INCLUSIVES.equals(node.getType())) {
                for (ProcessNode<?> branch : node.getBranchs()) {
                    ProcessNode<?> subNode = findSubNodeByIdFromRoot(nodeId, branch);
                    if (ObjectUtil.isNotNull(subNode)) {
                        WFlowToBpmnCreator.coverProps(node);
                        return subNode;
                    }
                }
            }
            return findSubNodeByIdFromRoot(nodeId, node.getChildren());
        }
        return null;
    }

    @Override
    public ProcessNode<?> findSubNodeByIdFromRoot(String nodeId, String code) {
        return findSubNodeByIdFromRoot(nodeId,
                JSONObject.parseObject(modelsMapper.selectById(code)
                        .getProcess(), ProcessNode.class));
    }

    @Override
    public ProcessNode<?> findSubNodeByDefIdFromRoot(String nodeId, String defId) {
        return findSubNodeByIdFromRoot(nodeId,
                JSONObject.parseObject(modelsMapper.selectOne(new LambdaQueryWrapper<WflowModels>()
                        .eq(WflowModels::getProcessDefId, defId)).getProcess(), ProcessNode.class));
    }

    @Override
    public void setProcessNodes(String code, Map<String, ProcessNode<?>> nodeMap) {
        processModelNodeMap.put(code, nodeMap);
    }

    @Override
    public Map<String, ProcessNode<?>> reloadProcessByCode(String code) {
        Map<String, ProcessNode<?>> nodeMap = new LinkedHashMap<>();
        loadProcess(JSONObject.parseObject(modelsMapper.selectById(code).getProcess(), ProcessNode.class), nodeMap);
        processModelNodeMap.put(code, nodeMap);
        return nodeMap;
    }

    @Override
    public Map<String, ProcessNode<?>> reloadProcessByStr(String process) {
        Map<String, ProcessNode<?>> nodeMap = new LinkedHashMap<>();
        loadProcess(JSONObject.parseObject(process, ProcessNode.class), nodeMap);
        return nodeMap;
    }

    @Override
    public void unloadProcessByCode(String code) {
        processModelNodeMap.remove(code);
    }

    private void loadProcess(ProcessNode<?> node, Map<String, ProcessNode<?>> nodeMap) {
        if (null != node && null != node.getId()) {
            WFlowToBpmnCreator.coverProps(node);
            nodeMap.put(node.getId(), node);
            if (NodeTypeEnum.CONCURRENTS.equals(node.getType())
                    || NodeTypeEnum.CONDITIONS.equals(node.getType())
                    || NodeTypeEnum.INCLUSIVES.equals(node.getType())) {
                node.getBranchs().forEach(n -> loadProcess(n, nodeMap));
            }
            loadProcess(node.getChildren(), nodeMap);
        }
    }
}

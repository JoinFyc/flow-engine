package com.wflow.workflow.service;

import com.wflow.workflow.bean.process.ProcessNode;

import java.util.List;
import java.util.Map;

/**
 * 流程节点缓存，快速获取某个流程的某节点设置项
 * @author : willian fu
 * @date : 2022/8/24
 */
public interface ProcessNodeCatchService {

    ProcessNode<?> getProcessNodeById(String code, String nodeId);

    ProcessNode<?> getProcessNode(String defId, String nodeId);

    Map<String, ProcessNode<?>> getProcessNode(String defId);

    <T> ProcessNode<T> getProcessNodeById(Class<T> clazz, String code, String nodeId);

    List<ProcessNode<?>> getTaskNodesByCode(String code);

    ProcessNode<?> findSubNodeByIdFromRoot(String nodeId, ProcessNode<?> root);

    ProcessNode<?> findSubNodeByIdFromRoot(String nodeId, String code);

    ProcessNode<?> findSubNodeByDefIdFromRoot(String nodeId, String defId);

    void setProcessNodes(String code, Map<String, ProcessNode<?>> nodeMap);

    Map<String, ProcessNode<?>> reloadProcessByCode(String code);

    Map<String, ProcessNode<?>> reloadProcessByStr(String process);

    void unloadProcessByCode(String code);
}

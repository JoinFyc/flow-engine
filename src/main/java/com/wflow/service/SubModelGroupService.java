package com.wflow.service;

import com.wflow.bean.entity.WflowSubGroups;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.bean.vo.WflowSubModelVo;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2023/11/26
 */
public interface SubModelGroupService {

    List<WflowSubGroups> getGroups();

    List<WflowSubProcess> getModels();

    void addGroup(String name);

    void deleteGroup(Long id);

    void updateGroup(WflowSubGroups group);

    void groupSort(List<Long> ids);

    WflowSubModelVo getModelDetail(String code);

    void deployModel(String id);

    String saveModel(WflowSubModelVo modelVo);

    void processSort(Long groupId, List<String> ids);

    void enableProcess(String id, Long groupId, boolean enable);

    void modelMoveToGroup(String id, Long groupId);
}

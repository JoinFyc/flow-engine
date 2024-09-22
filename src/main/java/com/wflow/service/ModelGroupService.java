package com.wflow.service;

import com.wflow.bean.entity.WflowModelGroups;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.vo.ModelGroupVo;
import com.wflow.bean.vo.remote.req.FlowModelGroupRequest;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/7/4
 */
public interface ModelGroupService {


    List<ModelGroupVo> getGroupModels(String userId, String modelName);

    List<ModelGroupVo> getGroupModels(String modelName);

    /**
     * 查询表单组
     *
     * @return 表单组数据
     */
    List<WflowModelGroups> getModelGroups();

    /**
     * 表单分组排序
     *
     * @param groups 分组数据
     * @return 排序结果
     */
    void modelGroupsSort(List<Long> groups);

    /**
     * 查询表单模板数据
     *
     * @param modelId 模板id
     * @return 模板详情数据
     */
    Object getModelById(String modelId);

    /**
     * 通过流程定义ID获取模型数据
     * @param defId 流程定义ID
     * @return 模板详情数据
     */
    Object getModelByDefId(String defId);

    /**
     * 修改分组
     *
     * @param id   分组ID
     * @param name 分组名
     * @return 修改结果
     */
    void updateModelGroupName(Long id, String name);

    /**
     * 新增表单分组
     *
     * @param name 分组名
     * @return 添加结果
     */
    void createModelGroup(String name);

    /**
     * 新增表单分组
     * @param request
     */
    void createModelGroup(FlowModelGroupRequest request);

    /**
     * 删除分组
     *
     * @param id 分组ID
     */
    void deleteModelGroup(Long id);

    /**
     * 删除分组,存在模型就不允许删除
     * @param id
     */
    void deleteByGroupId(Long id);

    /**
     * 分组流程排序
     * @param groupId 需要重排序的分组ID
     * @param modelIds 顺序模型排序ID
     */
    void groupModelSort(Long groupId, List<String> modelIds);

    /**
     * 删除流程模型
     * @param modelId 模型ID
     */
    void deleteModel(String modelId);

    /**
     * 移动模型到新分组
     * @param modelId 模型
     * @param groupId 目标分组
     */
    void modelMoveToGroup(String modelId, Long groupId);

    /**
     * 启用或停用流程模型
     * @param modelId 模型ID
     * @param active 是否启用
     */
    void enOrDisModel(String modelId, Boolean active);

    /**
     * 获取所有流程
     * @return 流程列表
     */
    List<WflowModels> getModelItem();
}

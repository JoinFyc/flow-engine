package com.wflow.controller;

import com.wflow.service.ModelGroupService;
import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2020/9/17
 */
@RestController
@RequestMapping("wflow/model")
public class ModelGroupController {

    @Autowired
    private ModelGroupService modelGroupService;


    /**
     * 查询所有分组流程模型数据
     * @param modelName 流程名称
     * @return 列表数据
     */
    @GetMapping("group/list")
    public Object getGroupModels(@RequestParam(required = false) String modelName) {
        return R.ok(modelGroupService.getGroupModels(null, modelName));
    }

    /**
     * 查询所有流程模型
     * @return 列表数据
     */
    @GetMapping("list")
    public Object getModelItem() {
        return R.ok(modelGroupService.getModelItem());
    }

    /**
     * 获取用户可见的流程列表
     * @param modelName 流程模型名筛选
     * @return 列表数据
     */
    @GetMapping("list/byUser")
    public Object getUserModels(@RequestParam(required = false) String modelName) {
        return R.ok(modelGroupService.getGroupModels(UserUtil.getLoginUserId(), modelName));
    }

    /**
     * 移动流程到新分组
     * @param modelId 流程模型
     * @param groupId 分组
     */
    @PutMapping("{modelId}/move/{groupId}")
    public Object modelMoveToGroup(@PathVariable String modelId,
                                 @PathVariable Long groupId) {
        modelGroupService.modelMoveToGroup(modelId, groupId);
        return R.ok("移动到新分组成功");
    }

    /**
     * 查询所有模型分组
     *
     * @return
     */
    @GetMapping("group")
    public Object getModelGroups() {
        return R.ok(modelGroupService.getModelGroups());
    }

    /**
     * 表单分组排序
     *
     * @param groups 分组数据
     * @return 排序结果
     */
    @PutMapping("group/sort")
    public Object modelGroupsSort(@RequestBody List<Long> groups) {
        modelGroupService.modelGroupsSort(groups);
        return R.ok("分组排序成功");
    }

    /**
     * 表单排序
     *
     * @param groupId 需要进行重排序的分组
     * @param modelIds 分组内表单排序ID
     * @return 排序结果
     */
    @PutMapping("sort/{groupId}")
    public Object groupModelSort(@PathVariable Long groupId,
                                 @RequestBody List<String> modelIds) {
        modelGroupService.groupModelSort(groupId, modelIds);
        return R.ok("移动位置成功");
    }

    /**
     * 修改分组
     *
     * @param groupId   分组ID
     * @param name 分组名
     * @return 修改结果
     */
    @PutMapping("group/{groupId}")
    public Object updateModelGroupName(@PathVariable Long groupId,
                                       @RequestParam String name) {
        modelGroupService.updateModelGroupName(groupId, name);
        return R.ok("修改分组名称成功");
    }

    @PutMapping("{modelId}/active/{type}")
    public Object enOrDisModel(@PathVariable String modelId,
                               @PathVariable Boolean type) {
        modelGroupService.enOrDisModel(modelId, type);
        return R.ok(Boolean.TRUE.equals(type) ? "停用流程成功":"启用流程成功");
    }

    /**
     * 新增表单分组
     *
     * @param name 分组名
     * @return 添加结果
     */
    @PostMapping("group")
    public Object createModelGroup(@RequestParam String name) {
        modelGroupService.createModelGroup(name);
        return R.ok("新增分组成功");
    }

    /**
     * 删除分组
     *
     * @param groupId 分组ID
     * @return 删除结果
     */
    @DeleteMapping("group/{groupId}")
    public Object deleteModelGroup(@PathVariable Long groupId) {
        modelGroupService.deleteModelGroup(groupId);
        return R.ok("删除分组成功");
    }

    /**
     * 删除流程模型
     * @param modelId id
     * @return 删除结果
     */
    @DeleteMapping("{modelId}")
    public Object deleteModel(@PathVariable String modelId) {
        modelGroupService.deleteModel(modelId);
        return R.ok("删除流程成功");
    }

    /**
     * 查询流程模型数据
     *
     * @param formId 模板id
     * @return 流程模型详情数据
     */
    @GetMapping("detail/{formId}")
    public Object getModelById(@PathVariable String formId) {
        return R.ok(modelGroupService.getModelById(formId));
    }

    /**
     * 通过流程定义ID查询流程
     * @param defId 流程部署后生成的定义ID
     * @return 流程
     */
    @GetMapping("detail/def/{defId}")
    public Object getModelByDefId(@PathVariable String defId) {
        return R.ok(modelGroupService.getModelByDefId(defId));
    }

}

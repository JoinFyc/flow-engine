package com.wflow.controller;

import com.wflow.bean.entity.WflowSubGroups;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.bean.vo.SubModelGroupVo;
import com.wflow.service.SubModelGroupService;
import com.wflow.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2023/11/26
 */
@RestController
@RequestMapping("wflow/model/sub")
public class SubModelGroupController {

    @Autowired
    private SubModelGroupService modelGroupService;

    /**
     * 管理子流程分组及流程列表
     * @return 子流程分组及流程列表
     */
    @GetMapping("group/list")
    public Object getGroupModels(){
        List<WflowSubGroups> groups = modelGroupService.getGroups();
        List<WflowSubProcess> models = modelGroupService.getModels();
        Map<Long, List<WflowSubProcess>> listMap = new HashMap<>(groups.size());
        models.forEach(v -> {
            List<WflowSubProcess> list = listMap.get(v.getGroupId());
            if (Objects.isNull(list)){
                list = new LinkedList<>();
                listMap.put(v.getGroupId(), list);
            }
            list.add(v);
        });
        return R.ok(groups.stream()
                .map(v -> new SubModelGroupVo(v.getGroupId(), v.getGroupName(),
                        listMap.getOrDefault(v.getGroupId(), Collections.emptyList())))
                .collect(Collectors.toList()));
    }

    /**
     * 修改子流程分组名称
     * @param groupId 分组id
     * @param name 分组名
     * @return 修改结果
     */
    @PutMapping("group/{groupId}")
    public Object updateGroup(@PathVariable Long groupId, @RequestParam String name){
        modelGroupService.updateGroup(WflowSubGroups.builder().groupId(groupId).groupName(name).build());
        return R.ok("修改分组成功");
    }

    /**
     * 查询子流程分组
     * @return 子流程分组列表
     */
    @GetMapping("group")
    public Object getGroups(){
        return R.ok(modelGroupService.getGroups());
    }

    /**
     * 添加子流程分组
     * @param name 分组名
     * @return 子流程分组
     */
    @PostMapping("group")
    public Object addGroup(@RequestParam String name){
        modelGroupService.addGroup(name);
        return R.ok("新增分组成功");
    }

    /**
     * 删除子流程分组
     * @param id 子流程分组ID
     * @return 删除结果
     */
    @DeleteMapping("group/{id}")
    public Object deleteGroup(@PathVariable Long id){
        modelGroupService.deleteGroup(id);
        return R.ok("删除分组成功");
    }

    /**
     * 按顺序给分组排序
     * @param ids 排序的分组id
     * @return 排序结果
     */
    @PutMapping("group/sort")
    public Object groupSort(@RequestBody List<Long> ids){
        modelGroupService.groupSort(ids);
        return R.ok("分组排序成功");
    }

    /**
     * 分组内的子流程排序
     * @param groupId 分组id
     * @param ids 子流程id列表
     * @return 排序结果
     */
    @PutMapping("sort/{groupId}")
    public Object processSort(@PathVariable Long groupId, @RequestBody List<String> ids){
        modelGroupService.processSort(groupId, ids);
        return R.ok("排序成功");
    }

    /**
     * 停用 / 启用子流程
     * @param id 子流程id
     * @param type 操作类型 true=启用 false=停用
     * @param groupId 子流程分组id
     * @return 操作结果
     */
    @PutMapping("{id}/active/{type}")
    public Object enOrDisModel(@PathVariable String id,
                               @PathVariable Boolean type,
                               @RequestParam(required = false) Long groupId) {
        modelGroupService.enableProcess(id, groupId, type);
        return R.ok(Boolean.TRUE.equals(type) ? "启用子流程成功":"停用子流程成功");
    }

    /**
     * 移动流程到新分组
     * @param id 流程模型
     * @param groupId 分组
     */
    @PutMapping("{id}/move/{groupId}")
    public Object modelMoveToGroup(@PathVariable String id,
                                   @PathVariable Long groupId) {
        modelGroupService.modelMoveToGroup(id, groupId);
        return R.ok("移动到新分组成功");
    }
}

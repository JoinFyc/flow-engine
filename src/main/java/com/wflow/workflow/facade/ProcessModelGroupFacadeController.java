package com.wflow.workflow.facade;

import com.wflow.bean.vo.remote.req.FlowModelGroupRequest;
import com.wflow.service.ModelGroupService;
import com.wflow.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * @author JoinFyc
 * @description 流程模型
 * @date 2024-08-27
 */
@Tag(name = "流程模型", description = "流程模型相关接口")
@RestController
@RequestMapping("/flow-engine/rest/model/group")
public class ProcessModelGroupFacadeController {

    @Resource
    private ModelGroupService modelGroupService;

    /**
     * 查询所有表单分组数据
     *
     * @return 列表数据
     */
    @GetMapping("list")
    @Operation(summary = "流程模型分组")
    public Object getModelGroups() {
        return R.ok(modelGroupService.getModelGroups());
    }

    /**
     * 新增表单分组
     *
     * @param request 分组信息
     * @return 添加结果
     */
    @PostMapping("add")
    @Operation(summary = "创建模型分组")
    public Object createModelGroup(@RequestBody FlowModelGroupRequest request) {
        modelGroupService.createModelGroup(request);
        return R.ok("新增分组成功");
    }

    /**
     * 删除分组,存在模型就不删除
     *
     * @param groupId 分组ID
     * @return 删除结果
     */
    @PostMapping("del")
    @Operation(summary = "删除模型分组")
    public Object deleteModelGroup(@RequestParam Long groupId) {
        modelGroupService.deleteByGroupId(groupId);
        return R.ok("删除分组成功");
    }

}

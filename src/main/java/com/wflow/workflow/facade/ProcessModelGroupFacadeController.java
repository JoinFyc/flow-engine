package com.wflow.workflow.facade;

import com.wflow.bean.FlowProcessContext;
import com.wflow.bean.vo.remote.req.FlowModelGroupRequest;
import com.wflow.service.ModelGroupService;
import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.vo.FormGroupVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * @author JoinFyc
 * @description 流程模型
 * @date 2024-08-27
 */
@Tag(name = "模型分类", description = "模型分类相关接口")
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
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("流程模型分组");
        flowProcessContext.setFieldTag(Boolean.TRUE);
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
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("创建模型分组");
        flowProcessContext.setFieldTag(Boolean.TRUE);
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
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("删除模型分组");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        modelGroupService.deleteByGroupId(groupId);
        return R.ok("删除分组成功");
    }

    /**
     * 获取用户可见的流程列表
     * @param modelName 流程模型名筛选
     * @return 列表数据
     */
    @GetMapping("list/byUser")
    public Object getUserModels(@RequestParam(required = false) String modelName) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("获取用户可见的流程列表");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        return R.ok(modelGroupService.getGroupModels(flowProcessContext.getUserId(), modelName));
    }

}

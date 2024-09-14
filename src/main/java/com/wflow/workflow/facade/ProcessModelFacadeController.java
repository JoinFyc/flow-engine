package com.wflow.workflow.facade;

import com.wflow.bean.FlowProcessContext;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.vo.remote.req.FlowModelSnapshotRequest;
import com.wflow.service.ModelGroupService;
import com.wflow.utils.R;
import com.wflow.workflow.service.ProcessModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author JoinFyc
 * @description 流程模型
 * @date 2024-08-27
 */
@Tag(name = "流程模型", description = "流程模型相关接口")
@RestController
@RequestMapping("/flow-engine/rest/model")
public class ProcessModelFacadeController {

    @Resource
    private ProcessModelService modelService;

    @Resource
    private ModelGroupService modelGroupService;

    /**
     * 保存流程，保存的是草稿不发布，会在history中生成新版本记录
     *
     * @param models 流程模型数据
     * @return 保存结果
     */
    @PostMapping("save")
    @Operation(summary = "保存流程")
    public Object saveProcess(@RequestBody FlowModelSnapshotRequest models) {
        final WflowModelHistorys modelHistory = new WflowModelHistorys();
        BeanUtils.copyProperties(models, modelHistory);
        modelHistory.setGroupId(Long.valueOf(models.getGroupId()));
        return R.ok(modelService.saveProcess(modelHistory));
    }

    /**
     * 通过流程ID 发布、部署流程
     *
     * @param formId 流程ID，也是流程定义KEY
     * @return 操作结果
     */
    @PostMapping("deploy")
    @Operation(summary = "发布流程")
    public Object deployModel(@RequestParam String formId) {
        modelService.deployProcess(formId);
        return R.ok("部署流程成功");
    }

    /**
     * 查询所有分组流程模型数据
     */
    @GetMapping("group")
    @Operation(summary = "查询所有分组流程模型数据")
    public Object getGroupModels() {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("不查询默认与停用分组");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        return R.ok(modelGroupService.getGroupModels(null, null));
    }
    /**
     * 查询流程模型数据
     * @param formId 模板id
     * @return 流程模型详情数据
     */
    @GetMapping("detail")
    @Operation(summary = "查询流程模型数据")
    public Object getModelById(@RequestParam String formId) {
        return R.ok(modelGroupService.getModelById(formId));
    }

}

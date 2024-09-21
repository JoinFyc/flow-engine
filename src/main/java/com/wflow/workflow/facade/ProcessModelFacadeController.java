package com.wflow.workflow.facade;

import com.wflow.bean.FlowProcessContext;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.vo.remote.req.FlowModelSnapshotRequest;
import com.wflow.service.ModelGroupService;
import com.wflow.utils.R;
import com.wflow.workflow.bean.vo.FormGroupVo;
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
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("保存流程");
        flowProcessContext.setFieldTag(Boolean.TRUE);
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
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("发布流程");
        flowProcessContext.setFieldTag(Boolean.TRUE);
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
        flowProcessContext.setFieldDesc("查询所有分组流程模型数据");
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
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("查询流程模型数据");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        return R.ok(modelGroupService.getModelById(formId));
    }
    /**
     * 通过流程模型ID（流程定义KEY）查询流程模型数据
     * @param code 流程模型ID（流程定义KEY）
     * @return 流程表单详情数据
     */
    @GetMapping("def")
    public Object getProcessModelByCode(@RequestParam String code){
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("通过流程模型ID（流程定义KEY）查询流程模型数据");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        return R.ok(modelService.getLastVersionModel(code));
    }

    /**
     * 停用流程成功 & 启用流程成功
     * @param modelId 流程模型ID
     * @param type 类型
     * @return 启用停用成功
     */
    @PutMapping("{modelId}/active/{type}")
    public Object enOrDisModel(@PathVariable String modelId,
                               @PathVariable Boolean type) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("停用流程成功 & 启用流程成功");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        modelGroupService.enOrDisModel(modelId, type);
        return R.ok(Boolean.TRUE.equals(type) ? "停用流程成功":"启用流程成功");
    }

    /**
     * 删除已部署的流程模型
     * @param formId 流程定义ID
     * @return 删除结果
     */
    @DeleteMapping("{formId}")
    public Object delProcessModel(@PathVariable String formId){
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("删除已部署的流程模型");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        modelService.delProcess(formId);
        return R.ok("删除流程成功");
    }

    /**
     * 表单分组排序
     *
     * @param formGroup 分组数据
     * @return 排序结果
     */
    @PostMapping("group/sort")
    public Object modelGroupsSort(@RequestBody FormGroupVo formGroup) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("表单分组排序");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        modelGroupService.modelGroupsSort(formGroup.getGroups());
        return R.ok("分组排序成功");
    }

}

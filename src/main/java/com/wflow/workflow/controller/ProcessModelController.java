package com.wflow.workflow.controller;

import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.bean.vo.WflowSubModelVo;
import com.wflow.service.SubModelGroupService;
import com.wflow.utils.R;
import com.wflow.workflow.service.ProcessModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author : JoinFyc
 * @date : 2024/8/22
 */
@RestController
@RequestMapping("wflow/process/model")
public class ProcessModelController {

    @Autowired
    private ProcessModelService modelService;

    @Autowired
    private SubModelGroupService subModelGroupService;

    /**
     * 保存流程，保存的是草稿不发布，会在history中生成新版本记录
     * @param models 流程模型数据
     * @return 保存结果
     */
    @PostMapping("save")
    public Object saveProcess(@RequestBody WflowModelHistorys models){
        return R.ok(modelService.saveProcess(models));
    }

    /**
     * 通过流程模型ID（流程定义KEY）查询流程模型数据
     * @param code 流程模型ID（流程定义KEY）
     * @return 流程表单详情数据
     */
    @GetMapping("{code}")
    public Object getProcessModelByCode(@PathVariable String code){
        return R.ok(modelService.getLastVersionModel(code));
    }

    //启用流程
    @PutMapping("enable/{code}")
    public Object enableProcess(@PathVariable String code){
        modelService.enableProcess(code, true);
        return R.ok("启用流程流程模板成功");
    }

    /**
     * 停用流程
     * @param code 流程ID，也是流程定义KEY
     * @return 停用结果
     */
    @PutMapping("disable/{code}")
    public Object disableProcess(@PathVariable String code){
        modelService.enableProcess(code, false);
        return R.ok("停用流程模板成功");
    }

    /**
     * 通过流程ID 发布、部署流程
     * @param code 流程ID，也是流程定义KEY
     * @return 操作结果
     */
    @PostMapping("deploy/{code}")
    public Object deployProcessModel(@PathVariable String code){
        modelService.deployProcess(code);
        return R.ok("部署流程成功");
    }

    /**
     * 删除已部署的流程模型
     * @param defId 流程定义ID
     * @return 删除结果
     */
    @DeleteMapping("{defId}")
    public Object delProcessModel(@PathVariable String defId){
        modelService.delProcess(defId);
        return R.ok("删除流程成功");
    }

    /**
     * 查询自定义打印模板配置
     * @param instanceId 流程实例ID
     * @return 打印配置
     */
    @GetMapping("customPrint/{instanceId}")
    public Object getCustomPrintConfig(@PathVariable String instanceId){
        return R.ok(modelService.getCustomPrintConfig(instanceId));
    }

    /**
     * 查询子流程模型
     * @param code 子流程编号
     * @return 子流程模型
     */
    @GetMapping("sub/{code}")
    public Object getModelDetail(@PathVariable String code){
        return R.ok(subModelGroupService.getModelDetail(code));
    }

    /**
     * 保存子流程设计
     * @param modelVo 模型设计信息
     * @return 子流程编号
     */
    @PostMapping("sub/save")
    public Object saveModel(@RequestBody WflowSubModelVo modelVo){
        return R.ok(subModelGroupService.saveModel(modelVo));
    }

    /**
     * 部署子流程
     * @param code 子流程编号
     * @return 部署结果
     */
    @PostMapping("sub/deploy/{code}")
    public Object publishModel(@PathVariable String code){
        subModelGroupService.deployModel(code);
        return R.ok("发布子流程模型成功");
    }

    /**
     * 保存并部署子流程
     * @param modelVo 模型设计信息
     * @return 子流程编号
     */
    @PostMapping("sub/deploy")
    public Object saveAndPublishModel(@RequestBody WflowSubModelVo modelVo){
        String id = subModelGroupService.saveModel(modelVo);
        subModelGroupService.deployModel(id);
        return R.ok("发布流程模型成功");
    }
}

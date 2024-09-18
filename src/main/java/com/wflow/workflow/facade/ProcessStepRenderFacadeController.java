package com.wflow.workflow.facade;

import com.wflow.bean.FlowProcessContext;
import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.vo.ProcessConditionResolveParamsVo;
import com.wflow.workflow.service.ProcessStepRenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author JoinFyc
 * @description 发起流程时的步骤节点数据获取接口
 * @date 2024-08-27
 */
@RestController
@RequestMapping("/flow-engine/rest/process/step")
public class ProcessStepRenderFacadeController {

    @Autowired
    private ProcessStepRenderService stepRenderService;

    /**
     * 解析网关分支条件，返回满足条件的分支
     * @param paramsVo 请求参数
     * @return 符合满足条件的分支id集合
     */
    @PostMapping("conditions/resolve")
    public Object getIsTrueConditions(@RequestBody ProcessConditionResolveParamsVo paramsVo){
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("解析网关分支条件，返回满足条件的分支");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        //注入发起人
        paramsVo.getContext().put("root", flowProcessContext.getUserId());
        return R.ok(stepRenderService.getIsTrueConditions(paramsVo));
    }

    @GetMapping("el/validate")
    public Object validateEl(@RequestParam String el){
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("验证el表达式");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        return R.ok(stepRenderService.validateEl(el));
    }
}

package com.wflow.workflow.controller;

import com.wflow.utils.R;
import com.wflow.workflow.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : JoinFyc
 * @date : 2024/8/24
 */
@RestController
@RequestMapping("wflow/process/form")
public class ProcessFromController {

    @Autowired
    private FormService formService;

    /**
     * 获取流程实例表单数据
     * @param instanceId 流程实例ID
     * @return 该流程实例的表单数据
     */
    @GetMapping("data/by/{instanceId}")
    public Object getProcessInstanceFormData(@PathVariable String instanceId){
        return R.ok(formService.getProcessInstanceFormData(instanceId));
    }

    /**
     * 获取流程实例表单字段历史数据
     * @param instanceId 流程实例ID
     * @param fieldId 字段ID
     * @return 该流程实例的表单字段历史数据
     */
    @GetMapping("his/by/{instanceId}/{fieldId}")
    public Object getProcessInstanceFormDataHis(@PathVariable String instanceId,
                                                @PathVariable String fieldId){
        return R.ok(formService.getFormDataChangeLog(instanceId, fieldId));
    }
}

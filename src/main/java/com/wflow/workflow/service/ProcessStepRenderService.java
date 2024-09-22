package com.wflow.workflow.service;

import com.wflow.workflow.bean.vo.ProcessConditionResolveParamsVo;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2023/11/10
 */
public interface ProcessStepRenderService {
    /**
     * 解析条件，判断是否满足
     * @param paramsVo 条件参数
     * @return 满足的分支的条件节点id
     */
    List<String> getIsTrueConditions(ProcessConditionResolveParamsVo paramsVo);
    /**
     * 校验el表达式语法
     * @param el 表达式
     * @return 校验结果
     */
    Boolean validateEl(String el);
}

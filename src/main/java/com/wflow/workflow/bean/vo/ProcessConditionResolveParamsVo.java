package com.wflow.workflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2023/11/10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessConditionResolveParamsVo {
    //流程定义ID
    private String processDfId;
    //条件网关节点ID，非条件的分支节点id哦
    private String conditionNodeId;
    //满足条件的分支取第一个还是取多个
    private Boolean multiple;
    //流程变量参数
    private Map<String, Object> context;
}

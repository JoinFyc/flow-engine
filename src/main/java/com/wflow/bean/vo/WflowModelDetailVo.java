package com.wflow.bean.vo;

import com.alibaba.fastjson2.JSONObject;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.form.Form;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/10/11
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WflowModelDetailVo {
    //流程ID
    private String formId;

    //流程分组类型
    private String groupType;

    //流程定义ID
    private String processDefId;

    /**
     * 表单名称
     */
    private String formName;
    /**
     * 图标配置
     */
    private String logo;

    /**
     * 表单设置内容
     */
    private List<Form> formItems;

    //表单配置
    private JSONObject formConfig;
    /**
     * 流程设置内容
     */
    private ProcessNode<?> process;
}

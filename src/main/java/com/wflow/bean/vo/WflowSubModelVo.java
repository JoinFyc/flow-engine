package com.wflow.bean.vo;

import com.alibaba.fastjson2.JSONObject;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.form.Form;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author : JoinFyc
 * @date : 2023/12/3
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WflowSubModelVo {
    //流程编号
    private String formId;

    //流程定义ID
    private String processDefId;
    /**
     * 表单名称
     */
    private String formName;

    //流程分组ID
    private Long groupId;

    //备注
    private String remark;
    /**
     * 流程设置内容
     */
    private String process;

    public WflowSubProcess cover(){
        return WflowSubProcess.builder()
                .procCode(formId)
                .procDefId(processDefId)
                .groupId(groupId)
                .process(process)
                .procName(formName)
                .remark(remark)
                .build();
    }
}

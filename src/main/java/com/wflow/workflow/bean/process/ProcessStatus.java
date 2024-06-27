package com.wflow.workflow.bean.process;

/**
 * 流程实例状态
 * @author : willian fu
 * @since : 2024/5/5
 */
public enum ProcessStatus {
    RUNNING, //进行中
    REFUSE, //审批被驳回
    PASS, //审批通过
    CANCEL //审批撤销
}

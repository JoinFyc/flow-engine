package com.wflow.workflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author : willian fu
 * @date : 2022/9/19
 */
@Data
@Builder
@AllArgsConstructor
public class InstanceCountVo {
    //待我处理
    private Long todo;
    //我提交并且未完成的
    private Long mySubmited;
    //抄送我的
    private Integer cc;
}

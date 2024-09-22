package com.wflow.workflow.bean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : JoinFyc
 * @date : 2024/8/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessInstanceOwnerDto implements Serializable {
    private static final long serialVersionUID = 457951701966938674L;
    //流程发起人ID
    private String owner;
    //流程发起人姓名
    private String ownerName;
    //流程发起人部门ID
    private String ownerDeptId;
    //流程发起人部门名称
    private String ownerDeptName;
}

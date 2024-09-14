package com.wflow.workflow.bean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * @author JoinFyc
 * @version : 1.0
 * @description 组织架构
 * @date 2024 09:14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgDto {

    private Set<String> deptIds;

    private String parentDeptId;

}

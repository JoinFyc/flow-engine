package com.wflow.bean.do_;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : JoinFyc
 * @date : 2024/08/29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeptDo {

    //部门ID
    private String id;
    /**
     * 部门名
     */
    private String deptName;
    /**
     * 部门主管
     */
    private String leader;

    private String parentId;
}

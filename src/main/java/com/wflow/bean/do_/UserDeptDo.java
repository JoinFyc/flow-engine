package com.wflow.bean.do_;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户及部门基本信息
 * @author : JoinFyc
 * @date : 2024/08/29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDeptDo {

    private String userId;

    private String deptId;

    private String avatar;

    private String deptName;

    private String userName;

    public UserDeptDo(String userId, String deptId) {
        this.userId = userId;
        this.deptId = deptId;
    }
}

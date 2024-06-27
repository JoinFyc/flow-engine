package com.wflow.bean.do_;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户及部门基本信息
 * @author : willian fu
 * @date : 2022/11/29
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

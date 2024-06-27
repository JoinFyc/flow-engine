package com.wflow.bean.vo;

import lombok.Data;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2024/4/2
 */
@Data
public class DeptRoleReqVo {
    private List<String> depts;
    private List<String> roles;
}

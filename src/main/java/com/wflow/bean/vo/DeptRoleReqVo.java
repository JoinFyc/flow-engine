package com.wflow.bean.vo;

import lombok.Data;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/4/2
 */
@Data
public class DeptRoleReqVo {
    private List<String> depts;
    private List<String> roles;
}

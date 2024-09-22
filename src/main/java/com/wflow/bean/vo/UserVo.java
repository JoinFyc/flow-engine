package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 用户详细信息实体，没有的可以不填
 * @author : JoinFyc
 * @date : 2024/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVo {

    private String userId;
    //用户名、姓名、昵称
    private String username;
    //头像
    private String avatar;
    //性别
    private Boolean sex;
    //手机号
    private String phoneNumber;
    //邮箱
    private String email;

    //所有岗位
    private List<String> positions;

    //拥有的角色
    private List<String> roles;

    //所在部门
    private List<String> depts;

    //入职日期
    private Date entryDate;

    //离职日期
    private Date leaveDate;
    //用户代理人
    private UserAgentVo userAgent;
}

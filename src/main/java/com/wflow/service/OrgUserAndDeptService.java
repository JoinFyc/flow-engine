package com.wflow.service;

import com.wflow.bean.vo.DeptVo;
import com.wflow.bean.vo.UserAgentVo;
import com.wflow.bean.vo.UserVo;
import com.wflow.workflow.bean.process.OrgUser;

import java.util.List;

/**
 * @author : willian fu
 * @version : 1.0
 */
public interface OrgUserAndDeptService {

    /**
     * 查询组织架构树
     * @param deptId 部门id
     * @param type 只查询部门架构
     * @return 组织架构树数据
     */
    Object getOrgTreeData(String deptId, String type);

    /**
     * 模糊搜索用户
     * @param userName 用户名/拼音/首字母
     * @return 匹配到的用户
     */
    Object getOrgTreeUser(String userName);

    /**
     * 查询用户的所有直属部门，一个人可能在多个部门下
     * @param userId 用户ID
     * @return 所在部门
     */
    List<DeptVo> getOrgUserDept(String userId);

    /**
     * 获取用户代理人
     * @return 代理人信息
     */
    UserAgentVo getUserAgent(String userId);

    /**
     * 设置用户代理人
     * @param agent 代理信息
     */
    void setUserAgent(UserAgentVo agent);

    /**
     * 清除用户审批代理人
     */
    void cleanUserAgent();

    /**
     * 查询用户保存的签名
     * @return 签名
     */
    String getUserSign();


    /**
     * 查询用户的详情
     * @param userId 用户id
     * @return 用户详细信息
     */
    UserVo getUserDetail(String userId);

}

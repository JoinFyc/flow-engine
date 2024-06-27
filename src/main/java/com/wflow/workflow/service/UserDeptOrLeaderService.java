package com.wflow.workflow.service;

import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.vo.DeptVo;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.OrgUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : willian fu
 * @date : 2022/8/22
 */
public interface UserDeptOrLeaderService {

    /**
     * 批量获取用户信息
     * @param userIds 用户ID
     * @return 用户id->基本信息
     */
    Map<String, OrgUser> getUserMapByIds(Collection<String> userIds);

    /**
     * 校验用户是否在某部门下
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是/否
     */
    boolean userIsBelongToDept(String userId, String deptId);

    /**
     * 校验部门是否属于某部门的子部门
     * @param deptId 子部门ID
     * @param parentDeptId 父部门ID
     * @return 是/否
     */
    boolean deptIsBelongToDept(String deptId, String parentDeptId);

    /**
     * 批量获取指定部门的主管
     * @param deptIds 部门ID列表
     * @return 部门主管列表
     */
    Set<String> getLeadersByDept(Collection<String> deptIds);

    /**
     * 获取用户指定级别的主管(用户可能在多个部门下)
     * @param userId 用户ID
     * @param userDept 用户的部门ID
     * @param level 级别
     * @param skipEmpty 为空是否也算
     * @return 主管
     */
    String getUserLeaderByLevel(String userId, String userDept, int level, boolean skipEmpty);

    /**
     * 查询用户的上级主管，直接向上
     * @param userDept 所在的直属部门，如果是本级部门领导，则是上级部门开始
     * @param skipEmpty 为空时是否跳过
     * @return 主管所在部门
     */
    DeptDo getUserDeptLeader(String userDept, boolean skipEmpty);

    /**
     * 获取用户到指定级别为止的所有主管
     * @param userId 用户ID
     * @param userDept 用户的部门ID
     * @param level 终止级别
     * @param skipEmpty 为空是否也算
     * @return 领导列表
     */
    List<String> getUserLeadersByLevel(String userId, String userDept, Integer level, boolean skipEmpty);

    /**
     * 获取用户的所有直属部门（一个人可能在多个部门下）
     * 为了在发起流程时候，选择以哪个部门身份发起
     * @param userId 用户
     * @return 所有的直属部门
     */
    List<DeptVo> getUserDepts(String userId);

    /**
     * 获取指定角色的用户
     * @param roles 角色列表
     * @return 符合要求的用户
     */
    Set<String> getUsersByRoles(List<String> roles);

    /**
     * 批量获取部门下用户
     * @param deptIds 部门ID集合
     * @return 部门下所有用户ID
     */
    Set<String> getUsersByDept(Collection<String> deptIds);

    /**
     * 批量获取用户审批代理人，没有代理人则为自己
     * @param userIds 用户ID集合
     * @return 用户审批代理人
     */
    List<String> replaceUserAsAgent(Collection<String> userIds);
}

package com.wflow.workflow.service;

import java.util.Set;

/**
 * 组织机构级联关系判别辅助服务，默认内存实现，也可以自行用redis实现本接口
 * @author : willian fu
 * @date : 2022/8/22
 */
public interface OrgOwnershipService {

    /**
     * 获取用户的部门归属关系
     * @param userId 用户ID
     * @return 部门归属关系
     */
    Set<String> getUserDepts(String userId);

    /**
     * 获取部门与部门级联归属关系，顺序级联
     * @param deptId 部门ID
     * @return 部门级联关系
     */
    Set<String> getDeptDepts(String deptId);

    /**
     * 重载用户与部门关系
     * @param userId 用户ID
     * @param deptId 部门ID
     * @param isRemove 是从部门移除还是加入部门
     */
    void reloadUserDept(String userId, String deptId, boolean isRemove);

    /**
     * 重载部门与部门关系
     * @param deptId 子部门ID
     * @param parent 父部门ID
     * @param isRemove 是从父部门移除还是加入父部门
     */
    void reloadDeptAndDept(String deptId, String parent, boolean isRemove);
}

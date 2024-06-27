package com.wflow.service;

import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.do_.RoleDo;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.WflowDepartments;
import com.wflow.bean.entity.WflowFormRecord;
import com.wflow.bean.vo.ModelGroupVo;
import com.wflow.bean.vo.OrgTreeVo;
import com.wflow.bean.vo.UserVo;
import com.wflow.workflow.bean.process.OrgUser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织架构管理服务，用来快速集成时实现本接口即可对接
 * @author : willian fu
 * @date : 2022/11/29
 */
public interface OrgRepositoryService {

    /**
     * 获取用户可见表单流程
     * @param userId 用户ID
     * @return 可见的表单流程
     */
    List<ModelGroupVo.Form> getModelsByPerm(String userId);

    /**
     * 通过ID获取用户
     * @param userId 用户ID
     * @return 用户信息
     */
    UserDo getUserById(String userId);

    /**
     * 通过拼音模糊搜索用户
     * @param py 拼音全拼或者首字母简拼
     * @return 搜索到的用户
     */
    List<OrgTreeVo> selectUsersByPy(String py);

    /**
     * 查询指定部门下的用户
     * @param deptId 药查询的部门ID
     * @return 部门内的直属用户
     */
    List<OrgTreeVo> selectUsersByDept(String deptId);

    /**
     * 通过用户ID批量查询用户信息
     * @param userIds 用户ID列表
     * @return 用户信息
     */
    List<UserDo> getUsersBatch(Collection<String> userIds);

    /**
     * 通过部门ID批量查询部门信息
     * @param deptIds 部门ID列表
     * @return 部门信息
     */
    List<DeptDo> getDeptBatch(Collection<String> deptIds);

    /**
     * 通过用户ID批量查询用户Map信息
     * @param userIds 用户ID列表
     * @return 用户信息Map
     */
    default Map<String, OrgUser> getUsersBatchMap(Collection<String> userIds){
        return this.getUsersBatch(userIds)
                .stream()
                .map(v -> new OrgUser(v.getUserId(), v.getUserName(), v.getAvatar(), "user", false))
                .collect(Collectors.toMap(OrgUser::getId, v -> v));
    }

    /**
     * 批量查询部门下的用户
     * @param deptIds 部门ID集合
     * @return 这些部门下的用户ID集合
     */
    Set<String> getUsersByDepts(Collection<String> deptIds);

    /**
     * 根据ID查询部门
     * @param deptId 部门ID
     * @return 部门信息
     */
    DeptDo getDeptById(String deptId);

    /**
     * 查询某用户的所有直属部门
     * @param userId 用户ID
     * @return 该用户的所有部门
     */
    List<DeptDo> getDeptsByUser(String userId);

    /**
     * 全量查询系统内所有部门
     * @return 所有部门
     */
    List<DeptDo> getSysAllDepts();

    /**
     * 全量查询系统内所有用户与部门关系
     * @return 所有部门
     */
    List<UserDeptDo> getSysAllUserDepts();

    /**
     * 根据ID查询父部门的所有直属子部门
     * @param parentId 父部门ID
     * @return 部门信息
     */
    List<OrgTreeVo> getSubDeptById(String parentId);

    /**
     * 指定一个部门，获取该部门下所有的子部门，包含内部的
     * @return 所有子部门ID
     */
    List<String> getRecursiveSubDept(String parentId);

    /**
     * 查询系统下所有角色
     * @return 角色列表
     */
    List<RoleDo> getSysAllRoles();

    /**
     * 批量获取拥有角色的用户
     * @param roles 角色列表
     * @return 用户ID
     */
    Set<String> getUsersByRoles(List<String> roles);

    /**
     * 查询指定用户的签名信息
     * @param userId 用户id
     * @return 签字信息
     */
    String getUserSign(String userId);

    /**
     * 更新用户签字信息
     * @param userId 用户id
     * @param signature 新的签字
     */
    void updateUserSign(String userId, String signature);

    /**
     * 查询用户的详细信息
     * @param userId 用户id
     * @return 用户详细信息
     */
    UserVo getUserDetail(String userId);

    /**
     * 查询用户及部门信息
     * @param userDeptIds 用户id + _ + 部门ID
     * @return 用户部门信息Map<用户id + _ + 部门ID, 信息>
     */
    default Map<String, UserDeptDo> getUserDeptInfos(Collection<String> userDeptIds){
        if (userDeptIds.isEmpty()){
            return Collections.emptyMap();
        }
        Set<String> users = new HashSet<>();
        Set<String> depts = new HashSet<>();
        //分离id
        userDeptIds.forEach(v -> {
            String[] s = v.split("_");
            users.add(s[0]);
            depts.add(s[1]);
        });
        //分开查询，对接的话只需要修改下面这个批量查询部门的就行
        Map<String, DeptDo> deptMap = getDeptBatch(depts).stream().collect(Collectors.toMap(DeptDo::getId, v -> v));
        Map<String, OrgUser> userMap = getUsersBatchMap(users);
        return userDeptIds.stream().map(v -> {
            String[] s = v.split("_");
            OrgUser user = userMap.getOrDefault(s[0], new OrgUser());
            DeptDo dept = deptMap.getOrDefault(s[1], new DeptDo());
            return new UserDeptDo(user.getId(), dept.getId(), user.getAvatar(), dept.getDeptName(), user.getName());
        }).collect(Collectors.toMap(v -> v.getUserId() + "_" + v.getDeptId(), v -> v));
    }
}

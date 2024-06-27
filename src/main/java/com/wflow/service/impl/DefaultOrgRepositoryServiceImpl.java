package com.wflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.do_.RoleDo;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.*;
import com.wflow.bean.vo.ModelGroupVo;
import com.wflow.bean.vo.OrgTreeVo;
import com.wflow.bean.vo.UserVo;
import com.wflow.mapper.*;
import com.wflow.service.OrgRepositoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认wflow的实现，集成自有系统需要自行实现接口
 * @author : willian fu
 * @date : 2022/11/29
 */
@Service
public class DefaultOrgRepositoryServiceImpl implements OrgRepositoryService {

    @Autowired
    private WflowUsersMapper usersMapper;

    @Autowired
    private WflowRolesMapper rolesMapper;

    @Autowired
    private WflowDepartmentsMapper departsMapper;

    @Autowired
    private WflowUserDepartmentsMapper userDepartmentsMapper;

    @Autowired
    private WflowUserRolesMapper userRolesMapper;

    @Autowired
    private WflowModelPermsMapper modelPermsMapper;

    @Override
    public List<ModelGroupVo.Form> getModelsByPerm(String userId) {
        return modelPermsMapper.selectByPerms(userId);
    }

    @Override
    public UserDo getUserById(String userId) {
        WflowUsers wflowUsers = usersMapper.selectById(userId);
        if (Objects.nonNull(wflowUsers)){
            UserDo userDo = new UserDo();
            BeanUtils.copyProperties(wflowUsers, userDo);
            return userDo;
        }
        return null;
    }

    @Override
    public List<OrgTreeVo> selectUsersByPy(String py) {
        return usersMapper.selectUsersByPy(py).stream()
                .peek(u -> u.setType("user")).collect(Collectors.toList());
    }

    @Override
    public List<OrgTreeVo> selectUsersByDept(String deptId) {
        List<OrgTreeVo> orgTreeVos = usersMapper.selectUsersByDept(deptId);
        orgTreeVos.forEach(v -> v.setType("user"));
        return orgTreeVos;
    }

    @Override
    public List<UserDo> getUsersBatch(Collection<String> userIds) {
        try {
            return usersMapper.selectBatchIds(userIds).stream()
                    .map(u -> new UserDo(u.getUserId(), u.getUserName(), u.getAvatar()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<DeptDo> getDeptBatch(Collection<String> deptIds) {
        try {
            return departsMapper.selectBatchIds(deptIds).stream()
                    .map(u -> new DeptDo(u.getId(), u.getDeptName(), u.getLeader(), u.getParentId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getUsersByDepts(Collection<String> deptIds) {
        try {
            return userDepartmentsMapper.selectList(
                    new LambdaQueryWrapper<WflowUserDepartments>()
                            .select(WflowUserDepartments::getUserId)
                            .in(WflowUserDepartments::getDeptId, deptIds))
                    .stream().map(WflowUserDepartments::getUserId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    @Override
    public DeptDo getDeptById(String deptId) {
        WflowDepartments departments = departsMapper.selectById(deptId);
        if (Objects.nonNull(departments)){
            DeptDo deptDo = new DeptDo();
            BeanUtils.copyProperties(departments, deptDo);
            return deptDo;
        }
        return null;
    }

    @Override
    public List<DeptDo> getSysAllDepts() {
        try {
            return departsMapper.selectList(null).stream()
                    .map(d -> new DeptDo(d.getId(), d.getDeptName(), d.getLeader(), d.getParentId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserDeptDo> getSysAllUserDepts() {
        try {
            return userDepartmentsMapper.selectList(null).stream()
                    .map(d -> new UserDeptDo(d.getUserId(), d.getDeptId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<OrgTreeVo> getSubDeptById(String parentId) {
        return departsMapper.selectList(new LambdaQueryWrapper<WflowDepartments>()
                .select(WflowDepartments::getId, WflowDepartments::getDeptName)
                .eq(WflowDepartments::getParentId, parentId))
                .stream().map(v -> OrgTreeVo.builder().id(v.getId())
                        .name(v.getDeptName())
                        .type("dept").build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRecursiveSubDept(String parentId) {
        List<String> list = new ArrayList<>();
        loadSubDept(parentId, list);
        return list;
    }

    /**
     * 递归加载所有子部门
     * @param parentId 父部门ID
     * @param subDepts 所有子部门缓存
     */
    private void loadSubDept(String parentId, List<String> subDepts){
        List<WflowDepartments> departments = departsMapper.selectList(
                new LambdaQueryWrapper<WflowDepartments>()
                .eq(WflowDepartments::getParentId, parentId));
        subDepts.addAll(departments.stream().map(WflowDepartments::getId).collect(Collectors.toList()));
        departments.forEach(d -> loadSubDept(d.getId(), subDepts));
    }

    @Override
    public List<RoleDo> getSysAllRoles() {
        try {
            return rolesMapper.selectList(null).stream()
                    .map(r -> new RoleDo(r.getRoleId(), r.getRoleName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getUsersByRoles(List<String> roles) {
        return userRolesMapper.selectList(new LambdaQueryWrapper<WflowUserRoles>()
                        .select(WflowUserRoles::getUserId)
                        .in(WflowUserRoles::getRoleId, roles)).stream()
                .map(WflowUserRoles::getUserId).collect(Collectors.toSet());
    }

    @Override
    public String getUserSign(String userId) {
        return usersMapper.selectById(userId).getSign();
    }

    @Override
    public void updateUserSign(String userId, String signature) {
        usersMapper.updateById(WflowUsers.builder().userId(userId).sign(signature).build());
    }

    @Override
    public UserVo getUserDetail(String userId) {
        WflowUsers user = usersMapper.selectById(userId);
        List<DeptDo> depts = getDeptsByUser(userId);
        List<WflowRoles> roles = rolesMapper.getRolesByUser(userId);
        return UserVo.builder()
                .userId(userId)
                .username(user.getUserName())
                .sex(user.getSex())
                .avatar(user.getAvatar())
                .entryDate(user.getEntryDate())
                .leaveDate(user.getLeaveDate())
                .positions(Collections.emptyList())
                .depts(depts.stream().map(DeptDo::getDeptName).collect(Collectors.toList()))
                .roles(roles.stream().map(WflowRoles::getRoleName).collect(Collectors.toList()))
                .build();
    }

    @Override
    public List<DeptDo> getDeptsByUser(String userId) {
        return userDepartmentsMapper.getUserDepts(userId);
    }
}

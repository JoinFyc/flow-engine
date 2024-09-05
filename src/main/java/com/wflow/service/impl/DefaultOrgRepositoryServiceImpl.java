package com.wflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.FlowProcessContext;
import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.do_.RoleDo;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.*;
import com.wflow.bean.vo.ModelGroupVo;
import com.wflow.bean.vo.OrgTreeVo;
import com.wflow.bean.vo.UserVo;
import com.wflow.exception.BusinessException;
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

    @Autowired
    private HrmStaffInfoMapper hrmStaffInfoMapper;

    @Autowired
    private OrgDeptMapper orgDeptMapper;

    @Override
    public List<ModelGroupVo.Form> getModelsByPerm(String userId) {
        return modelPermsMapper.selectByPerms(userId);
    }

    @Override
    public UserDo getUserById(String userId) {
        if(FlowProcessContext.getFlowProcessContext() == null) {
            WflowUsers wflowUsers = usersMapper.selectById(userId);
            if (Objects.nonNull(wflowUsers)){
                UserDo userDo = new UserDo();
                BeanUtils.copyProperties(wflowUsers, userDo);
                return userDo;
            }
        }
        final HrmStaffInfo hrmStaffInfo = hrmStaffInfoMapper.selectById(Long.valueOf(userId));
        UserDo userDo = new UserDo();
        userDo.setUserId(userId);
        userDo.setUserName(hrmStaffInfo.getUserName());
        userDo.setAvatar(hrmStaffInfo.getPersonalPhoto());
        return userDo;
    }

    @Override
    public List<OrgTreeVo> selectUsersByPy(String py) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.getFlowProcessContext();
        if(flowProcessContext != null && flowProcessContext.getFieldTag()) {
           return hrmStaffInfoMapper.selectUsersLikeName(py).stream()
                    .peek(u -> u.setType("user")).collect(Collectors.toList());
        }
        return usersMapper.selectUsersByPy(py).stream()
                .peek(u -> u.setType("user")).collect(Collectors.toList());
    }

    @Override
    public List<OrgTreeVo> selectUsersByDept(String deptId) {
        List<OrgTreeVo> orgTreeVos = FlowProcessContext.getFlowProcessContext() == null ?
                usersMapper.selectUsersByDept(deptId) :
                hrmStaffInfoMapper.selectUsersByDept(Long.valueOf(deptId));
        orgTreeVos.forEach(v -> v.setType("user"));
        return orgTreeVos;
    }

    @Override
    public List<UserDo> getUsersBatch(Collection<String> userIds) {
        try {
            return FlowProcessContext.getFlowProcessContext() == null ? usersMapper.selectBatchIds(userIds).stream()
                    .map(u -> new UserDo(u.getUserId(), u.getUserName(), u.getAvatar()))
                    .collect(Collectors.toList()) :
                    hrmStaffInfoMapper.selectBatchIds(userIds).stream()
                            .map(u -> new UserDo(u.getAutoNo().toString(), u.getUserName(), u.getPersonalPhoto()))
                            .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<DeptDo> getDeptBatch(Collection<String> deptIds) {
        try {
            return FlowProcessContext.getFlowProcessContext() == null ? departsMapper.selectBatchIds(deptIds).stream()
                    .map(u -> new DeptDo(u.getId(), u.getDeptName(), u.getLeader(), u.getParentId()))
                    .collect(Collectors.toList()) :
                    orgDeptMapper.selectBatchIds(deptIds).stream()
                            .map(u -> new DeptDo(u.getAutoNo().toString(), u.getName(), u.getResponsibleId() == null ? "" : u.getResponsibleId().toString(), u.getParentDeptId() == null ? "": u.getParentDeptId().toString()))
                            .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getUsersByDepts(Collection<String> deptIds) {
        try {
            return FlowProcessContext.getFlowProcessContext() == null ? userDepartmentsMapper.selectList(
                    new LambdaQueryWrapper<WflowUserDepartments>()
                            .select(WflowUserDepartments::getUserId)
                            .in(WflowUserDepartments::getDeptId, deptIds))
                    .stream().map(WflowUserDepartments::getUserId)
                    .collect(Collectors.toSet()) :
                    hrmStaffInfoMapper.selectList(
                            new LambdaQueryWrapper<HrmStaffInfo>()
                                    .select(HrmStaffInfo::getAutoNo)
                                    .in(HrmStaffInfo::getDeptNo, deptIds.stream().map(Long::parseLong).collect(Collectors.toSet()))
                    ).stream().map(s -> s.getAutoNo().toString()).collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    @Override
    public DeptDo getDeptById(String deptId) {
        if(FlowProcessContext.getFlowProcessContext() == null) {
            WflowDepartments departments = departsMapper.selectById(deptId);
            if (Objects.nonNull(departments)) {
                DeptDo deptDo = new DeptDo();
                BeanUtils.copyProperties(departments, deptDo);
                return deptDo;
            }
        }
        final OrgDept orgDept = orgDeptMapper.selectById(Long.valueOf(deptId));
        if (Objects.nonNull(orgDept)) {
            DeptDo deptDo = new DeptDo();
            deptDo.setId(orgDept.getAutoNo().toString());
            deptDo.setDeptName(orgDept.getName());
            deptDo.setLeader(orgDept.getResponsibleId() == null ? "" : orgDept.getResponsibleId().toString());
            deptDo.setParentId(orgDept.getParentDeptId() == null ? "" : orgDept.getParentDeptId().toString());
            return deptDo;
        }
        return null;
    }

    @Override
    public List<DeptDo> getSysAllDepts() {
        try {
            return FlowProcessContext.getFlowProcessContext() == null ? departsMapper.selectList(null).stream()
                    .map(d -> new DeptDo(d.getId(), d.getDeptName(), d.getLeader(), d.getParentId()))
                    .collect(Collectors.toList()) :
                    orgDeptMapper.selectList(null).stream()
                            .map(d -> new DeptDo(d.getAutoNo().toString(), d.getName(), d.getResponsibleId() == null ? "" : d.getResponsibleId().toString(), d.getParentDeptId().toString()))
                            .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserDeptDo> getSysAllUserDepts() {
        try {
            return FlowProcessContext.getFlowProcessContext() == null? userDepartmentsMapper.selectList(null).stream()
                    .map(d -> new UserDeptDo(d.getUserId(), d.getDeptId()))
                    .collect(Collectors.toList()) :
                    hrmStaffInfoMapper.selectList(null).stream()
                            .map(d -> new UserDeptDo(d.getAutoNo().toString(), d.getDeptNo().toString()))
                            .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<OrgTreeVo> getDeptByCoNo(Long coNo) {
        return  orgDeptMapper.selectList(new LambdaQueryWrapper<OrgDept>()
                                .select(OrgDept::getAutoNo, OrgDept::getName)
                                .eq(OrgDept::getCoNo, coNo))
                        .stream().map(v -> OrgTreeVo.builder().id(v.getAutoNo().toString())
                                .name(v.getName())
                                .type("dept").build())
                        .collect(Collectors.toList());
    }

    @Override
    public List<OrgTreeVo> getSubDeptById(String parentId) {
        return FlowProcessContext.getFlowProcessContext() == null?
                departsMapper.selectList(new LambdaQueryWrapper<WflowDepartments>()
                .select(WflowDepartments::getId, WflowDepartments::getDeptName)
                .eq(WflowDepartments::getParentId, parentId))
                .stream().map(v -> OrgTreeVo.builder().id(v.getId())
                        .name(v.getDeptName())
                        .type("dept").build())
                .collect(Collectors.toList())
                :
                orgDeptMapper.selectList(new LambdaQueryWrapper<OrgDept>()
                                .select(OrgDept::getAutoNo, OrgDept::getName)
                                .eq(OrgDept::getParentDeptId, parentId))
                        .stream().map(v -> OrgTreeVo.builder().id(v.getAutoNo().toString())
                                .name(v.getName())
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
        if(FlowProcessContext.getFlowProcessContext() == null) {
            List<WflowDepartments> departments = departsMapper.selectList(
                    new LambdaQueryWrapper<WflowDepartments>()
                    .eq(WflowDepartments::getParentId, parentId));
            subDepts.addAll(departments.stream().map(WflowDepartments::getId).collect(Collectors.toList()));
            departments.forEach(d -> loadSubDept(d.getId(), subDepts));
        }else {
            List<OrgDept> departments = orgDeptMapper.selectList(
                    new LambdaQueryWrapper<OrgDept>()
                            .eq(OrgDept::getParentDeptId, Long.valueOf(parentId)));
            subDepts.addAll(departments.stream().map(d -> d.getAutoNo().toString()).collect(Collectors.toList()));
            departments.forEach(d -> loadSubDept(d.getAutoNo().toString(), subDepts));
        }
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
        if (FlowProcessContext.getFlowProcessContext() == null) {
            WflowUsers user = usersMapper.selectById(userId);
            List<DeptDo> depts = getDeptsByUser(userId);
            List<WflowRoles> roles = rolesMapper.getRolesByUser(userId);
            return  UserVo.builder()
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
        final HrmStaffInfo hrmStaffInfo = hrmStaffInfoMapper.selectById(Long.valueOf(userId));
        if(hrmStaffInfo==null){
            throw new BusinessException("员工信息不存在");
        }
        final List<DeptDo> depts = getDeptsByUser(userId);
        return  UserVo.builder()
                .userId(userId)
                .username(hrmStaffInfo.getUserName())
                .sex(hrmStaffInfo.getGender() == 0)
                .avatar(hrmStaffInfo.getPersonalPhoto())
//                .entryDate(hrmStaffInfo.getEntryDate())
//                .leaveDate(hrmStaffInfo.getLeaveDate())
                .positions(Collections.emptyList())
                .depts(depts.stream().map(DeptDo::getDeptName).collect(Collectors.toList()))
//                .roles(roles.stream().map(WflowRoles::getRoleName).collect(Collectors.toList()))
                .build();

    }

    @Override
    public List<DeptDo> getDeptsByUser(String userId) {
        if(FlowProcessContext.getFlowProcessContext()==null){
            return userDepartmentsMapper.getUserDepts(userId);
        }
        final HrmStaffInfo hrmStaffInfo = hrmStaffInfoMapper.selectById(Long.valueOf(userId));
        if(hrmStaffInfo==null){
            throw new BusinessException("员工信息不存在");
        }
        return orgDeptMapper.selectList(
                new LambdaQueryWrapper<OrgDept>()
                        .select(OrgDept::getAutoNo,OrgDept::getName)
                        .eq(OrgDept::getAutoNo,hrmStaffInfo.getAutoNo()
                )).stream().map(d ->
                    DeptDo.builder()
                        .id(d.getAutoNo().toString())
                        .deptName(d.getName())
                        .build()
        ).collect(Collectors.toList());
    }
}

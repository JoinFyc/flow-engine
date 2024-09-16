package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.*;
import com.wflow.bean.vo.DeptVo;
import com.wflow.mapper.*;
import com.wflow.service.OrgRepositoryService;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.service.OrgOwnershipService;
import com.wflow.workflow.service.UserDeptOrLeaderService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2022/8/22
 */
@Slf4j
@Service
public class UserDeptOrLeaderServiceImpl implements UserDeptOrLeaderService {

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    @Autowired
    private WflowUserAgentsMapper userAgentsMapper;

    @Autowired
    private OrgOwnershipService orgOwnershipService;

    @Override
    public Map<String, OrgUser> getUserMapByIds(Collection<String> userIds) {
        return orgRepositoryService.getUsersBatch(userIds)
                        .stream().map(us -> OrgUser.builder()
                        .id(us.getUserId())
                        .name(us.getUserName())
                        .avatar(us.getAvatar())
                        .build())
                .collect(Collectors.toMap(OrgUser::getId, v -> v));
    }

    /**
     * 校验用户是否在某部门下
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是/否
     */
    @Override
    public boolean userIsBelongToDept(String userId, String deptId) {
//        TODO 校验用户是否在某部门下
        Set<String> set = orgOwnershipService.getUserDepts(userId);
        return CollectionUtil.isNotEmpty(set) && set.contains(deptId);
    }

    /**
     * 校验部门是否属于某部门的子部门
     *
     * @param deptId       子部门ID
     * @param parentDeptId 父部门ID
     * @return 是/否
     */
    @Override
    public boolean deptIsBelongToDept(String deptId, String parentDeptId) {
//        TODO 校验部门是否属于某部门的子部门
        Set<String> set = orgOwnershipService.getDeptDepts(deptId);
        return CollectionUtil.isNotEmpty(set) && set.contains(parentDeptId);
    }

    @Override
    public Set<String> getLeadersByDept(Collection<String> deptIds) {
        Set<String> set = new LinkedHashSet<>();
        if (CollectionUtil.isNotEmpty(deptIds)){
            deptIds.forEach(d -> {
                DeptDo dept = orgRepositoryService.getDeptById(d);
                if (Objects.nonNull(dept) && StrUtil.isNotBlank(dept.getLeader())){
                    set.add(dept.getLeader());
                }
            });
        }
        return set;
    }

    @Override
    public synchronized String getUserLeaderByLevel(String userId, String userDept, int level, boolean skipEmpty) {
        try {
            DeptDo department = null;
            //获取起始部门
            String parentDept = getUserParentDept(userId, userDept);
            do {
                department = getUserDeptLeader(parentDept, skipEmpty);
                parentDept = Objects.nonNull(department) ? department.getParentId() : null;
            }while (--level >= 1);
            return Objects.nonNull(department) ? department.getLeader() : null;
        } catch (Exception e) {
            log.error("获取用户[{}]部门[{}]指定级别[{}]领导异常", userId, userDept, level, e);
            return null;
        }
    }

    @Override
    public DeptDo getUserDeptLeader(String userDept, boolean skipEmpty) {
        if (StrUtil.isBlank(userDept)){
            return null;
        }
        DeptDo department = orgRepositoryService.getDeptById(userDept);
        if (skipEmpty && Objects.nonNull(department) && StrUtil.isBlank(department.getLeader())){
            return getUserDeptLeader(department.getParentId(), skipEmpty);
        }
        return department;
    }

    @Override
    public synchronized List<String> getUserLeadersByLevel(String userId, String userDept, Integer level, boolean skipEmpty) {
        List<String> leaders = new ArrayList<>();
        try {
            DeptDo department = null;
            //获取起始部门
            String parentDept = getUserParentDept(userId, userDept);
            do {
                department = getUserDeptLeader(parentDept, skipEmpty);
                if (Objects.isNull(department)){
                    break;
                }
                leaders.add(department.getLeader());
                parentDept = department.getParentId();
            }while ((level == 0 && StrUtil.isNotBlank(parentDept)) || --level >= 1);
        } catch (Exception e) {
            log.error("取用户[{}]部门[{}]的直到[{}]级Leader异常 {}", userId, userDept, level, e);
            return leaders;
        }
        //过滤掉本人及空
        return leaders.stream().filter(us -> Objects.nonNull(us) && !us.equals(userId)).collect(Collectors.toList());
    }

    @Override
    public List<DeptVo> getUserDepts(String userId) {
        return null;
    }

    @Override
    public Set<String> getUsersByRoles(List<String> roles) {
        return orgRepositoryService.getUsersByRoles(roles);
    }

    @Override
    public Set<String> getUsersByDept(Collection<String> deptIds) {
        //TODO 此处可以优化取部门算法，因为可能选择了父部门又选了子部门，这时候按父部门来处理，但是一般不会选很多部门
        return orgRepositoryService.getUsersByDepts(deptIds);
    }

    @Override
    public List<String> replaceUserAsAgent(Collection<String> userIds) {
        Date time = GregorianCalendar.getInstance().getTime();
        List<WflowUserAgents> agents = userAgentsMapper.selectList(
                new LambdaQueryWrapper<WflowUserAgents>()
                .select(WflowUserAgents::getAgentUserId, WflowUserAgents::getUserId)
                .ge(WflowUserAgents::getEndTime, time)
                .le(WflowUserAgents::getStartTime, time)
                .in(WflowUserAgents::getUserId, userIds));
        Map<String, String> agentMap = agents.stream()
                .collect(Collectors.toMap(WflowUserAgents::getUserId, WflowUserAgents::getAgentUserId));
        return userIds.stream().map(u -> agentMap.getOrDefault(u, u)).collect(Collectors.toList());
    }

    /**
     * 获取用户直属部门ID，如果为本部门主管，则直属部门为上级部门
     * @param userId 用户ID
     * @param deptId 当前部门ID
     * @return 直属部门
     */
    private String getUserParentDept(String userId, String deptId){
        if (StrUtil.isNotBlank(deptId)){
            DeptDo department = orgRepositoryService.getDeptById(deptId);
            if (userId.equals(department.getLeader())){
                return department.getParentId();
            }
            return deptId;
        }
        return null;
    }
}

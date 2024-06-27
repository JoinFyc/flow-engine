package com.wflow.workflow.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.wflow.bean.vo.DeptRoleReqVo;
import com.wflow.service.OrgUserAndDeptService;
import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.UELTools;
import com.wflow.workflow.bean.dto.OrgCompareDto;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.vo.ProcessConditionResolveParamsVo;
import com.wflow.workflow.service.ProcessStepRenderService;
import com.wflow.workflow.service.UserDeptOrLeaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 发起流程时的步骤节点数据获取接口
 * @author : willian fu
 * @date : 2022/9/29
 */
@RestController
@RequestMapping("wflow/process/step")
public class ProcessStepRenderController {

    @Autowired
    private UserDeptOrLeaderService userDeptOrLeaderService;

    @Autowired
    private ProcessStepRenderService stepRenderService;

    /**
     * 判断用户是否属于某部门
     * @param deptIds 部门id集合
     * @return 判断结果
     */
    @PostMapping("user/{userId}/belong/depts")
    public Object getUserBelongToDepts(@PathVariable String userId,
                                       @RequestBody List<String> deptIds){
        for (String deptId : deptIds) {
            if (userDeptOrLeaderService.userIsBelongToDept(userId, deptId)){
                return R.ok(true);
            }
        }
        return R.ok(false);
    }

    /**
     * 判断多个人员都是指定部门内的人员
     * @param compares 参数，指定用户何部门
     * @return 比较结果
     */
    @PostMapping("users/belong/depts")
    public Object getUsersBelongToDepts(@RequestBody OrgCompareDto compares){
        for (String sourceId : compares.getSourceIds()) {
            boolean result = false;
            for (String deptId : compares.getTargetIds()) {
                if (userDeptOrLeaderService.userIsBelongToDept(sourceId, deptId)){
                    result = true;
                    break;
                }
            }
            if (!result){
                return R.ok(false);
            }
        }
        return R.ok(true);
    }

    /**
     * 判断多个部门都是指定部门内的子部门/相等也算
     * @param compares 参数，指定的部门
     * @return 比较结果
     */
    @PostMapping("depts/belong/depts")
    public Object getDeptsBelongToDepts(@RequestBody OrgCompareDto compares){
        for (String sourceId : compares.getSourceIds()) {
            boolean result = false;
            for (String deptId : compares.getTargetIds()) {
                if (sourceId.equals(deptId) || userDeptOrLeaderService.deptIsBelongToDept(sourceId, deptId)){
                    result = true;
                    break;
                }
            }
            if (!result){
                return R.ok(false);
            }
        }
        return R.ok(true);
    }

    /**
     * 获取指定角色的用户
     * @param roleIds 角色id
     * @return 用户列表
     */
    @PostMapping("userByRoles")
    public Object getUsersByRoles(@RequestBody List<String> roleIds){
        Set<String> users = userDeptOrLeaderService.getUsersByRoles(roleIds);
        if (!users.isEmpty()){
            Map<String, OrgUser> userMap = userDeptOrLeaderService.getUserMapByIds(users);
            return R.ok(users.stream().map(us -> {
                OrgUser orgUser = userMap.get(us);
                Optional.ofNullable(orgUser).ifPresent(u -> u.setType("user"));
                return orgUser;
            }).collect(Collectors.toList()));
        }
        return R.ok(Collections.emptyList());
    }

    /**
     * 获取指定部门内角色的用户
     * @param deptRoles 部门角色信息参数
     * @return 用户列表
     */
    @PostMapping("deptUserByRoles")
    public Object getDeptUsersByRoles(@RequestBody DeptRoleReqVo deptRoles){
        Set<String> users = userDeptOrLeaderService.getUsersByRoles(deptRoles.getRoles())
                .stream().filter(userId -> {
                    for (String dept : deptRoles.getDepts()) {
                        if (userDeptOrLeaderService.userIsBelongToDept(userId, dept)) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toSet());
        if (!users.isEmpty()){
            Map<String, OrgUser> userMap = userDeptOrLeaderService.getUserMapByIds(users);
            return R.ok(users.stream().map(us -> {
                OrgUser orgUser = userMap.get(us);
                Optional.ofNullable(orgUser).ifPresent(u -> u.setType("user"));
                return orgUser;
            }).collect(Collectors.toList()));
        }
        return R.ok(Collections.emptyList());
    }

    /**
     * 通过部门ID搜索所有主管主管
     * @param deptIds 部门ID列表
     * @return 所有的部门主管
     */
    @PostMapping("deptLeader")
    public Object getLeadersByDepts(@RequestBody List<String> deptIds){
        Set<String> leaders = userDeptOrLeaderService.getLeadersByDept(deptIds);
        if (!leaders.isEmpty()){
            Map<String, OrgUser> userMap = userDeptOrLeaderService.getUserMapByIds(leaders);
            return R.ok(leaders.stream().map(us -> {
                OrgUser orgUser = userMap.get(us);
                Optional.ofNullable(orgUser).ifPresent(u -> u.setType("user"));
                return orgUser;
            }).collect(Collectors.toList()));
        }
        return R.ok(Collections.emptyList());
    }

    /**
     * 获取指定用户级别的leader
     * @param level 指定级别
     * @param deptId 用户的部门ID
     * @param skipEmpty 是否跳过空节点
     * @return 用户的主管
     */
    @GetMapping("leader/level")
    public Object getUserLeaderByLeave(@RequestParam Integer level,
                                       @RequestParam String deptId,
                                       @RequestParam Boolean skipEmpty){
        String leader = userDeptOrLeaderService.getUserLeaderByLevel(UserUtil.getLoginUserId(), deptId, level, skipEmpty);
        if (StrUtil.isNotBlank(leader)){
            OrgUser user = userDeptOrLeaderService.getUserMapByIds(CollectionUtil.newArrayList(leader)).get(leader);
            Optional.ofNullable(user).ifPresent(u -> u.setType("user"));
            return R.ok(user);
        }
        return null;
    }

    /**
     * 获取指定用户级别的所有leader
     * @param maxLevel 指定级别
     * @param deptId 用户的部门ID
     * @param skipEmpty 是否跳过空节点
     * @return 用户的所有主管
     */
    @GetMapping("leader/to/level")
    public Object getUserLeadersByLeave(@RequestParam Integer maxLevel,
                                        @RequestParam String deptId,
                                        @RequestParam Boolean skipEmpty){
        List<String> leaders = userDeptOrLeaderService.getUserLeadersByLevel(UserUtil.getLoginUserId(), deptId, maxLevel, skipEmpty);
        if (CollectionUtil.isNotEmpty(leaders)){
            Map<String, OrgUser> map = userDeptOrLeaderService.getUserMapByIds(leaders);
            return R.ok(leaders.stream().map(u -> {
                OrgUser user = map.get(u);
                user.setType("user");
                return user;
            }).collect(Collectors.toList()));
        }
        return R.ok(Collections.emptyList());
    }

    /**
     * 解析网关分支条件，返回满足条件的分支
     * @param paramsVo 请求参数
     * @return 符合满足条件的分支id集合
     */
    @PostMapping("conditions/resolve")
    public Object getIsTrueConditions(@RequestBody ProcessConditionResolveParamsVo paramsVo){
        //注入发起人
        paramsVo.getContext().put("root", UserUtil.getLoginUserId());
        return R.ok(stepRenderService.getIsTrueConditions(paramsVo));
    }

    @GetMapping("el/validate")
    public Object validateEl(@RequestParam String el){
        return R.ok(stepRenderService.validateEl(el));
    }
}

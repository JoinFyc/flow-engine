package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.bean.vo.DeptVo;
import com.wflow.service.OrgRepositoryService;
import com.wflow.service.OrgUserAndDeptService;
import com.wflow.workflow.service.OrgOwnershipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存维护组织机构的用户与部门归属关系
 * 牺牲空间换时间，建立所有 部门->部门、用户->部门 的级联映射
 * @author : willian fu
 * @date : 2022/8/22
 */
@Slf4j
@Service
public class MemoryOrgOwnershipServiceImpl implements OrgOwnershipService {

    //用户ID与其所有层级所属部门ID级联映射
    private static final Map<String, Set<String>> userDeptMap = new ConcurrentHashMap<>();

    //部门ID与其所有父级部门ID级联关系映射
    private static final Map<String, Set<String>> deptAndDeptMap = new ConcurrentHashMap<>();

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    @Autowired
    private OrgUserAndDeptService userAndDeptService;

    /**
     * 获取用户的部门归属关系
     * @param userId 用户ID
     * @return 部门归属关系
     */
    @Override
    public Set<String> getUserDepts(String userId) {
        return userDeptMap.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * 获取部门与部门级联归属关系，顺序级联
     * @param deptId 部门ID
     * @return 部门级联关系
     */
    @Override
    public Set<String> getDeptDepts(String deptId) {
        return deptAndDeptMap.getOrDefault(deptId, Collections.emptySet());
    }

    /**
     * 重载用户与部门关系
     * @param userId 用户ID
     * @param deptId 部门ID
     * @param isRemove 是从部门移除还是加入部门
     */
    @Override
    public void reloadUserDept(String userId, String deptId, boolean isRemove) {
        if (isRemove){
            userDeptMap.remove(userId);
        }else {
            //获取用户的直属部门
            List<DeptVo> userDepts = userAndDeptService.getOrgUserDept(userId);
            HashSet<String> set = new LinkedHashSet<>();
            userDepts.forEach(ud -> {
                //加载级联父级部门
                set.addAll(deptAndDeptMap.getOrDefault(ud.getId(), new LinkedHashSet<>()));
            });
            userDeptMap.put(userId, set);
        }
    }

    /**
     * 重载部门与部门关系
     * @param deptId 子部门ID
     * @param parent 父部门ID
     * @param isRemove 是从父部门移除还是加入父部门
     */
    @Override
    public void reloadDeptAndDept(String deptId, String parent, boolean isRemove) {
        this.loadByDbToCatch();
    }

    /**
     * 实例化时从数据库加载组织架构关系
     */
    @PostConstruct
    public void loadByDbToCatch(){
        //查询全量数据
        log.info("开始加载全量组织架构关系数据");
        List<UserDeptDo> userDepartments = orgRepositoryService.getSysAllUserDepts();
        List<DeptDo> departments = orgRepositoryService.getSysAllDepts();
        userDeptMap.clear();
        deptAndDeptMap.clear();
        //先加载部门级联关系，部门map化
        Map<String, DeptDo> deptMap = departments.stream().collect(Collectors.toMap(DeptDo::getId, v -> v));
        deptMap.forEach((k, v) -> {
            Set<String> set = new LinkedHashSet<>();
            loadCascade(set, deptMap, v);
            deptAndDeptMap.put(k, set);
        });
        //再加载用户与所属部门的级联关系
        userDepartments.forEach(ud -> {
            Set<String> userDeptSet = userDeptMap.get(ud.getUserId());
            if (CollectionUtil.isEmpty(userDeptSet)){
                userDeptSet = new LinkedHashSet<>();
                userDeptMap.put(ud.getUserId(), userDeptSet);
            }
            userDeptSet.add(ud.getDeptId());
            userDeptSet.addAll(deptAndDeptMap.getOrDefault(ud.getDeptId(), new HashSet<>()));
        });
    }

    /**
     * 递归加载所有部门级联关系
     * @param set 级联缓存
     * @param deptMap 部门map
     * @param dept 当前部门
     */
    private void loadCascade(Set<String> set, Map<String, DeptDo> deptMap, DeptDo dept){
        if (ObjectUtil.isNotNull(dept) && ObjectUtil.isNotNull(dept.getParentId())){
            set.add(dept.getParentId());
            loadCascade(set, deptMap, deptMap.get(dept.getParentId()));
        }
    }
}

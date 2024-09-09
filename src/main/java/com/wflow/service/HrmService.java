package com.wflow.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.HrmStaffInfo;
import com.wflow.bean.vo.OrgTreeVo;
import com.wflow.mapper.HrmStaffInfoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@DS("hr")
public class HrmService {

    @Resource
    private HrmStaffInfoMapper hrmStaffInfoMapper;

    public List<OrgTreeVo> selectUsersByDept(Long deptId){
        return hrmStaffInfoMapper.selectUsersByDept(deptId);
    }

    public List<OrgTreeVo> selectUsersLikeName(String userName){
        return hrmStaffInfoMapper.selectUsersLikeName(userName);
    }

    public HrmStaffInfo selectByUserId(Long id){
        return hrmStaffInfoMapper.selectByUserId(id);
    }

    public List<HrmStaffInfo> selectBatchIds(Collection ids) {
        return hrmStaffInfoMapper.selectBatchIds(ids);
    }

    public Set<String> selectList(Collection<String> deptIds) {
        return  hrmStaffInfoMapper.selectList(
                new LambdaQueryWrapper<HrmStaffInfo>()
                        .select(HrmStaffInfo::getAutoNo)
                        .in(HrmStaffInfo::getDeptNo, deptIds.stream().map(Long::parseLong).collect(Collectors.toSet()))
        ).stream().map(s -> s.getAutoNo().toString()).collect(Collectors.toSet());
    }

    public List<HrmStaffInfo> selectList() {
        return hrmStaffInfoMapper.selectList(null);
    }
}

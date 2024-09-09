package com.wflow.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wflow.bean.entity.OrgDept;
import com.wflow.mapper.OrgDeptMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@DS("hr")
public class OrgService {

    @Resource
    private OrgDeptMapper orgDeptMapper;

    public List<OrgDept> selectBatchIds(Collection<String> deptIds) {
        return   orgDeptMapper.selectBatchIds(deptIds);
    }

    public OrgDept selectById(Long deptId) {
        return orgDeptMapper.selectById(deptId);
    }

    public List<OrgDept> selectList(Wrapper<OrgDept> wrapper) {
        return orgDeptMapper.selectList(wrapper);
    }
}

package com.wflow.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.OrgDept;

/**
 * @author : willian fu
 * @date : 2022/7/4
 */
@DS("hr")
public interface OrgDeptMapper extends BaseMapper<OrgDept> {
}

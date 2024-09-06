package com.wflow.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.HrmStaffInfo;
import com.wflow.bean.entity.PositionInfo;
import com.wflow.bean.vo.OrgTreeVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/7/4
 */
@DS("hr")
public interface PositionInfoMapper extends BaseMapper<PositionInfo> {


}

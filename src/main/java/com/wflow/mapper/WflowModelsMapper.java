package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.vo.ModelGroupVo;
import org.apache.ibatis.annotations.Select;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/6/27
 */
public interface WflowModelsMapper extends BaseMapper<WflowModels> {

    @Select("SELECT * FROM wflow_models WHERE is_delete = 0 ORDER BY group_id ASC, sort ASC")
    List<ModelGroupVo.Form> getSysModels();
}

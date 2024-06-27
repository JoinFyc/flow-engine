package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowSubProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2023/11/27
 */
public interface WflowSubProcessMapper extends BaseMapper<WflowSubProcess> {

    /**
     * mysql8支持rownumber函数，可以直接 row_number() over (partition by proc_code order by version desc) num
     * 查询已发布的最新版本的所有子流程
     * @return 子流程列表
     */
    @Select("SELECT * FROM wflow_sub_process wsb, " +
            "( SELECT proc_code, max( version ) version FROM wflow_sub_process WHERE is_deleted = 0 GROUP BY proc_code ) tb " +
            "WHERE tb.version = wsb.version AND tb.proc_code = wsb.proc_code ORDER BY wsb.sort ASC")
    List<WflowSubProcess> getModelList();

    /**
     * 查询最新版本的子流程
     * @param code 子流程编号
     * @return 子流程模型
     */
    @Select("SELECT * FROM wflow_sub_process wsb, " +
            "( SELECT proc_code, max( version ) version FROM wflow_sub_process WHERE is_deleted = 0 AND is_stop = 0 GROUP BY proc_code ) tb " +
            "WHERE tb.version = wsb.version AND tb.proc_code = wsb.proc_code AND wsb.proc_code = #{code} ORDER BY wsb.sort ASC")
    WflowSubProcess getLastVerModel(@Param("code") String code);
}

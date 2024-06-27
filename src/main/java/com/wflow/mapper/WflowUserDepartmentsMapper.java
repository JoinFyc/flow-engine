package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.bean.entity.WflowUserDepartments;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/6/27
 */
public interface WflowUserDepartmentsMapper extends BaseMapper<WflowUserDepartments> {

    /**
     * 查询指定用户所在的部门，一个用户可能同时在多个部门下
     *
     * @param userId 用户ID
     * @return 用户所在的部门列表
     */
    @Select("SELECT wd.id, wd.dept_name FROM wflow_user_departments wud, wflow_departments wd WHERE wud.dept_id = wd.id AND wud.user_id = #{userId}")
    List<DeptDo> getUserDepts(@Param("userId") String userId);

    /**
     * 批量查询指定用户及部门信息
     *
     * @param udIds 用户ID_部门id 字符串拼接的集合
     * @return 用户部门信息列表
     */
    @Select({"<script>",
            "SELECT tb.* FROM (SELECT CONCAT(wu.user_id, '_', wd.id) tid, wu.user_id, wu.user_name, wd.id dept_id, wu.avatar, wd.dept_name",
            "FROM wflow_users wu, wflow_departments wd, wflow_user_departments wud",
            "WHERE wu.user_id = wud.user_id AND wd.id = wud.dept_id) tb WHERE tb.tid IN",
            "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>#{item}</foreach>",
            "</script>"})
    List<UserDeptDo> getUserDepInfosBatch(@Param("list") Collection<String> udIds);
}

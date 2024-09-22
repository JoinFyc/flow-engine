package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowUsers;
import com.wflow.bean.vo.OrgTreeVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/6/27
 */
public interface WflowUsersMapper extends BaseMapper<WflowUsers> {

    /**
     * 查询该部门下的所有用户
     * @param deptId 部门ID
     * @return 用户列表 type为固定值user
     */
    @Select("SELECT ou.user_id id, ou.user_name name, ou.avatar " +
            "FROM wflow_user_departments oud LEFT JOIN wflow_users ou ON ou.user_id = oud.user_id " +
            "WHERE oud.dept_id = #{deptId}")
    List<OrgTreeVo> selectUsersByDept(@Param("deptId") String deptId);

    /**
     * 通过拼音搜索用户，全拼和拼音首字母模糊搜索
     * @param py 拼音
     * @return 搜索的用户列表 type为固定值user
     */
    @Select("SELECT ou.user_id id, ou.user_name name, ou.avatar FROM wflow_users ou " +
            "WHERE ou.user_name LIKE '%${py}%' OR pingyin LIKE '%${py}%' OR py LIKE '%${py}%'")
    List<OrgTreeVo> selectUsersByPy(@Param("py") String py);
}

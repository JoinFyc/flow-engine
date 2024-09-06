package com.wflow.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.HrmStaffInfo;
import com.wflow.bean.vo.OrgTreeVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/7/4
 */
@DS("hr")
public interface HrmStaffInfoMapper extends BaseMapper<HrmStaffInfo> {


    /**
     * 查询该部门下的所有用户
     * @param deptId 部门ID
     * @return 用户列表 type为固定值user
     */
    @Select("SELECT ou.auto_no id, ou.staff_name name, ou.personal_photo  avatar FROM hrm_staff_info ou " +
            "WHERE ou.dept_no = #{deptId} ")
    List<OrgTreeVo> selectUsersByDept(@Param("deptId") Long deptId);


    /**
     * 通过名称搜索用户，用户名和昵称模糊搜索
     *
     * @param userName
     * @return 搜索的用户列表 type为固定值user
     */
    @Select("SELECT ou.auto_no id, ou.user_name name, ou.personal_photo  avatar FROM hrm_staff_info ou " +
            "WHERE ou.user_name LIKE '%${userName}%' OR ou.nick_name LIKE '%${userName}%' ")
    List<OrgTreeVo> selectUsersLikeName(@Param("userName") String userName);


}

package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.HrmStaffInfo;
import com.wflow.bean.vo.OrgTreeVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/7/4
 */
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
     * 根据userId查询
     * @param userId
     * @return
     */
    @Select("select user_name,personal_photo FROM hrm_staff_info where auto_no = #{userId}")
    HrmStaffInfo selectByUserId(@Param("userId") Long userId);

    @Select("select auto_no,user_name,personal_photo FROM hrm_staff_info " +
            "where auto_no in " +
            "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>#{item}</foreach> ")
    List<HrmStaffInfo> selectBatchIds(@Param("list") Collection<? extends Serializable> ids);

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

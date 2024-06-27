package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowRoles;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/6/27
 */
public interface WflowRolesMapper extends BaseMapper<WflowRoles> {

    /**
     * 查询用户的所有角色信息
     * @param userId 用户ID
     * @return 用户拥有的角色列表
     */
    @Select("SELECT wr.* FROM wflow_roles wr, wflow_user_roles wur " +
            "WHERE wr.role_id = wur.role_id AND wur.user_id = ${userId}")
    List<WflowRoles> getRolesByUser(@Param("userId") Object userId);
}

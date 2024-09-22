package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowModelPerms;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.vo.ModelGroupVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/08/29
 */
public interface WflowModelPermsMapper extends BaseMapper<WflowModelPerms> {

    @Insert({"<script>" +
            "INSERT INTO wflow_model_perms (id, form_id, perm_type, org_id, create_time) VALUES\n" +
            "  <foreach collection =\"perms\" item=\"t\" separator =\",\">\n" +
            "    (#{t.id}, #{t.formId}, #{t.permType}, #{t.orgId}, #{t.createTime})\n" +
            "  </foreach >" +
            "</script>"})
    int insertBatch(@Param("perms") List<WflowModelPerms> tasks);

    @Insert({"<script>" +
            "BEGIN "+
            "  <foreach collection =\"perms\" item=\"t\" separator =\";\">\n" +
            "   INSERT INTO wflow_model_perms (id, form_id, perm_type, org_id, create_time) VALUES\n" +
            "    (#{t.id}, #{t.formId}, #{t.permType}, #{t.orgId}, #{t.createTime})\n" +
            "  </foreach > ;" +
            "END; " +
            "</script>"})
    int insertOracleBatch(@Param("perms") List<WflowModelPerms> tasks);

    @Select("SELECT * FROM wflow_models WHERE is_delete = 0 AND is_stop = 0 AND form_id IN (\n" +
            "\tSELECT form_id FROM wflow_model_perms WHERE perm_type = 'user' AND org_id = #{userId}\n" +
            "\tUNION All SELECT p.form_id FROM wflow_model_perms p, wflow_user_departments d \n" +
            "\tWHERE p.org_id = d.dept_id AND p.perm_type = 'dept' AND d.user_id = #{userId}\n" +
            ") OR form_id NOT IN (SELECT form_id FROM wflow_model_perms) ORDER by group_id ASC, sort ASC")
    List<ModelGroupVo.Form> selectByPerms(@Param("userId") String userId);
}

package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowCcTasks;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/9/8
 */
public interface WflowCcTasksMapper extends BaseMapper<WflowCcTasks> {

    @Insert({"<script>" +
            "INSERT INTO wflow_cc_tasks (id, instance_id, user_id, node_id, code, node_name, create_time) VALUES\n" +
            "  <foreach collection =\"tasks\" item=\"t\" separator =\",\">\n" +
            "    (#{t.id}, #{t.instanceId}, #{t.userId}, #{t.nodeId}, #{t.code}, #{t.nodeName}, #{t.createTime})\n" +
            "  </foreach >" +
            "</script>"})
    int insertBatch(@Param("tasks") List<WflowCcTasks> tasks);

    @Insert({"<script>" +
            "BEGIN "+
            "  <foreach collection =\"tasks\" item=\"t\" separator =\";\">\n" +
            "     INSERT INTO WFLOW_CC_TASKS (id,instance_id, user_id, node_id, code, node_name, create_time) VALUES\n" +
            "    (#{t.id},#{t.instanceId}, #{t.userId}, #{t.nodeId}, #{t.code}, #{t.nodeName}, #{t.createTime})\n" +
            "  </foreach > ;" +
            "END; " +
            "</script>"})
    int insertOracleBatch(@Param("tasks") List<WflowCcTasks> tasks);
}

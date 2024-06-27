package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowFormRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/9/8
 */
public interface WflowFormRecordMapper extends BaseMapper<WflowFormRecord> {

    /**
     * 批量插入表单记录
     * @param records 表单记录
     * @return 插入数量
     */
    @Insert({"<script>" +
            "INSERT INTO wflow_form_record (id, instance_id, field_id, old_value, new_value, create_time, update_by) VALUES\n" +
            "  <foreach collection =\"records\" item=\"t\" separator =\",\">\n" +
            "    (#{t.id}, #{t.instanceId}, #{t.fieldId}, #{t.oldValue}, #{t.newValue}, #{t.createTime}, #{t.updateBy})\n" +
            "  </foreach >" +
            "</script>"})
    int insertBatch(@Param("records") List<WflowFormRecord> records);

    @Insert({"<script>" +
            "BEGIN "+
            "  <foreach collection =\"records\" item=\"t\" separator =\";\">\n" +
            "    INSERT INTO wflow_form_record (id, instance_id, field_id, old_value, new_value, create_time, update_by) VALUES\n" +
            "    (#{t.id}, #{t.instanceId}, #{t.fieldId}, #{t.oldValue}, #{t.newValue}, #{t.createTime}, #{t.updateBy})\n" +
            "  </foreach > ;" +
            "END; " +
            "</script>"})
    int insertOracleBatch(@Param("records") List<WflowFormRecord> records);
}

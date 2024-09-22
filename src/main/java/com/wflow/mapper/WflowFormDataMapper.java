package com.wflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wflow.bean.entity.WflowFormData;
import com.wflow.bean.entity.WflowFormRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/9/8
 */
public interface WflowFormDataMapper extends BaseMapper<WflowFormData> {

    /**
     * 批量插入表单数据
     * @param datas 表单数据
     * @return 插入数量
     */
    @Insert({"<script>" +
            "INSERT INTO wflow_form_data (id, instance_id, code, def_id, field_id, field_key, field_name, field_type, is_json, field_value, create_time) VALUES\n" +
            "  <foreach collection =\"datas\" item=\"t\" separator =\",\">\n" +
            "    (#{t.id}, #{t.instanceId}, #{t.code}, #{t.defId}, #{t.fieldId}, #{t.fieldKey}, #{t.fieldName}, #{t.fieldType}, #{t.isJson}, #{t.fieldValue}, #{t.createTime})\n" +
            "  </foreach>" +
            "</script>"})
    int insertBatch(@Param("datas") List<WflowFormData> datas);

    @Insert({"<script>" +
            "BEGIN " +
            "  <foreach collection =\"datas\" item=\"t\" separator =\"; \">\n" +
            "    INSERT INTO WFLOW_FORM_DATA (id, instance_id, code, def_id, field_id, field_key, field_name, field_type, is_json, field_value, create_time) VALUES\n" +
            "    (#{t.id}, #{t.instanceId}, #{t.code}, #{t.defId}, #{t.fieldId}, #{t.fieldKey}, #{t.fieldName}, #{t.fieldType}, #{t.isJson}, #{t.fieldValue}, #{t.createTime})\n" +
            "  </foreach> ;" +
            "END; "+
            "</script>"})
    int insertOracleBatch(@Param("datas") List<WflowFormData> datas);
}

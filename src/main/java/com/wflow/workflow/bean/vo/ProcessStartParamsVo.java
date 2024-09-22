package com.wflow.workflow.bean.vo;

import com.wflow.workflow.bean.process.OrgUser;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/8/23
 */
@Data
public class ProcessStartParamsVo {
    //发起部门
    private String deptId;

    //表单数据 字段ID -> 字段值
    private Map<String, Object> formData;

    //流程节点ID -> 流程选择的人员
    private Map<String, List<OrgUser>> processUsers;

    //流程唯一业务ID
    private String flowUniqueId;
}

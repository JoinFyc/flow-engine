package com.wflow.workflow.bean.process.props;

import com.alibaba.fastjson2.JSONArray;
import com.wflow.workflow.bean.process.OperationPerm;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.form.FormPerm;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/7/7
 */
@Data
public class RootProps implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;

    private List<OrgUser> assignedUser;
    private List<FormPerm> formPerms;
    private OperationPerm operationPerm;
    private Map<String, JSONArray> listeners;
}

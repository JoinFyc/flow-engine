package com.wflow.workflow.bean.process.props;

import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.form.FormPerm;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/7/7
 */
@Data
public class CcProps implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;

    private Boolean shouldAdd;
    private List<OrgUser> assignedUser;
    private List<FormPerm> formPerms;
}

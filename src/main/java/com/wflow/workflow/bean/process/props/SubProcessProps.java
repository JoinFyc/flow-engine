package com.wflow.workflow.bean.process.props;

import com.wflow.workflow.bean.process.form.FormPerm;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author : willian fu
 * @date : 2023/11/23
 */
@Data
public class SubProcessProps  implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;

    private String subProcCode;

    private List<FormPerm> formPerms;

    private StaterUser staterUser;

    private String startDept;

    private Boolean subAll;

    private Boolean syncVersion;

    private List<Var> inVar;

    private List<Var> outVar;

    @Data
    public static class StaterUser implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;

        private String type;

        private Object value;
    }

    @Data
    public static class Var implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        //主流程变量key
        private String mKey;
        //子流程变量key
        private String sKey;
    }
}

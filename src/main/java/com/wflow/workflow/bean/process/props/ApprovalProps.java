package com.wflow.workflow.bean.process.props;

import com.alibaba.fastjson2.JSONArray;
import com.wflow.workflow.bean.process.OperationPerm;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.enums.ApprovalModeEnum;
import com.wflow.workflow.bean.process.enums.ApprovalTypeEnum;
import com.wflow.workflow.bean.process.form.FormPerm;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/7/6
 */
@Data
public class ApprovalProps implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;

    private ApprovalTypeEnum assignedType;

    private ApprovalModeEnum mode;

    private boolean sign;

    private Nobody nobody;

    private TimeLimit timeLimit;

    private List<OrgUser> assignedUser;

    private List<OrgUser> assignedDept;

    private SelfSelect selfSelect;

    private LeaderTop leaderTop;

    private Leader leader;

    private List<OrgUser> role;

    private String formUser;

    private String formDept;

    private Refuse refuse;

    private List<FormPerm> formPerms;

    private Map<String, JSONArray> listeners;

    private List<String> assignedNode;

    private OperationPerm operationPerm;

    private DeptProp deptProp;

    @Data
    public static class DeptProp implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private String type; //ALL=全部人员 / LEADER=主管 / ROLE=部门下的角色
        private List<OrgUser> roles;
    }

    @Data
    public static class Nobody implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private NobodyHandlerTypeEnum handler;
        private List<OrgUser> assignedUser;
    }

    public enum NobodyHandlerTypeEnum {
        TO_PASS, TO_REFUSE, TO_ADMIN, TO_USER
    }

    @Data
    public static class TimeLimit implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private TimeOut timeout;
        private Handler handler;

        @Data
        public static class TimeOut implements Serializable {
            private static final long serialVersionUID = -45475579271153023L;
            private String unit;
            private Integer value;
        }

        @Data
        public static class Handler implements Serializable {
            private static final long serialVersionUID = -45475579271153023L;
            private HandlerType type;
            private Notify notify;

            public enum HandlerType{
                PASS, REFUSE, NOTIFY
            }

            @Data
            public static class Notify implements Serializable {
                private static final long serialVersionUID = -45475579271153023L;
                private boolean once;
                private Integer hour;
            }
        }
    }

    @Data
    public static class SelfSelect implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private boolean multiple;
    }

    @Data
    public static class LeaderTop implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private String endCondition;
        private Integer endLevel;
        private Boolean skipEmpty = false;
    }

    @Data
    public static class Leader implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private Integer level;
        private Boolean skipEmpty = true;
    }

    @Data
    public static class Role implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private String id;
        private String name;
    }

    @Data
    public static class Refuse implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private String type;
        private String target;
    }
}

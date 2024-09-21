package com.wflow.workflow.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.wflow.workflow.bean.process.OrgUser;

import java.util.regex.Pattern;

/**
 * @author : willian fu
 * @date : 2022/9/4
 */
public class WflowGlobalVarDef {
    //数据库类型，在启动时加载进去
    public static DbType DB_TYPE = DbType.MYSQL;

    //审批自动驳回
    public static final String WFLOW_TASK_REFUSE = "WFLOW_TASK_REFUSE";
    //审批自动通过
    public static final String WFLOW_TASK_AGRRE = "WFLOW_TASK_AGRRE";

    //流程Node节点变量KEY
    public static final String WFLOW_NODE_PROPS = "WFLOW_NODE_PROPS";

    //最近一个审批的节点
    public static final String PREVIOUS_AP_NODE = "PREVIOUS_AP_NODE";
    //流程出现回退及节点驳回的标记
    public static final String NODE_RETURN = "NODE_RETURN";
    //表单变量KEY
    public static final String WFLOW_FORMS = "WFLOW_FORMS";
    //部门变量名
    public static final String START_DEPT = "startDept";
    //发起人信息变量名
    public static final String OWNER = "owner";

    //任务处理结果变量前缀
    public static final String TASK_RES_PRE = "approve_";

    //任务事件前缀
    public static final String TASK_EVENT_PRE = "event_";

    //有事件触发的最终审批人标识
    public static final String LAST_AUDIT_EVENT_TAG = "last_audit_event_tag_";

    //系统审批管理员角色
    public static final String WFLOW_APPROVAL_ADMIN = "WFLOW_APPROVAL_ADMIN";

    //默认系统作为审批用户
    public static final OrgUser SYS = OrgUser.builder().id("WFLOW_SYS")
            .avatar("https://dd-static.jd.com/ddimg/jfs/t1/154957/17/25841/2492/631f2ca9Edc4615eb/9745007fe2540577.png")
            .name("系统").type("user").build();

    //流程发起人变量
    public static final String INITIATOR = "initiator";

    //流程唯一ID
    public static final String FLOW_UNIQUE_ID = "flow_unique_id";

    //模板变量替换正则编译
    public static final Pattern TEMPLATE_REPLACE_REG = Pattern.compile("\\$\\{(.+?)\\}");

}

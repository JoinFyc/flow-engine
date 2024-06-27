package com.wflow.workflow.config.callActivity;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.props.SubProcessProps;
import com.wflow.workflow.config.WflowGlobalVarDef;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.CallActivityBehavior;

import java.util.List;
import java.util.Map;

/**
 * 重写调用子流程的发起人信息设置逻辑，参考源码中 CallActivityBehavior
 * @author : willian fu
 * @date : 2023/12/19
 */
@Slf4j
public class WflowCallActivityBehavior extends CallActivityBehavior {

    public static final ThreadLocal<ProcessInstanceOwnerDto> OWNER = new ThreadLocal<>();

    public WflowCallActivityBehavior(CallActivity callActivity) {
        super(callActivity);
    }

    @Override
    public void execute(DelegateExecution execution) {
        initStartUserInfo(execution);
        super.execute(execution);
        Authentication.setAuthenticatedUserId(null);
    }

    private void initStartUserInfo(DelegateExecution execution){
        //读取子流程设置项
        Map<String, Object> nodes = (Map<String, Object>) execution.getVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS);
        SubProcessProps props = (SubProcessProps)nodes.get(execution.getCurrentActivityId());
        //提取子流程发起人信息
        String startUser = String.valueOf(execution.getVariable(WflowGlobalVarDef.INITIATOR));
        SubProcessProps.StaterUser staterUser = props.getStaterUser();
        String startDept = String.valueOf(execution.getVariable(WflowGlobalVarDef.START_DEPT));
        //先设置默认的发起人信息
        ProcessInstanceOwnerDto owner = ProcessInstanceOwnerDto.builder().ownerDeptId(startDept).owner(startDept).build();
        switch (props.getStaterUser().getType()){
            case "ROOT":
                break;
            case "FORM": //从选人表单组件取子流程发起人
                List<OrgUser> users = (List<OrgUser>) execution.getVariable(staterUser.getValue().toString());
                startUser = users.get(0).getId();
                break;
            case "SELECT": //自选发起人
                startUser = ((JSONObject)staterUser.getValue()).getString("id");
                break;
        }
        owner.setOwner(startUser);
        if (StrUtil.isNotBlank(props.getStartDept())){
            owner.setOwnerDeptId(props.getStartDept());
        }
        OWNER.set(owner); //设置到缓存
        Authentication.setAuthenticatedUserId(startUser);
        log.info("设置子流程[{}]发起人[{}]", execution.getCurrentActivityId(), startUser);
    }
}

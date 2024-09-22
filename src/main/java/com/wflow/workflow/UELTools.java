package com.wflow.workflow;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import com.wflow.bean.do_.UserDeptDo;
import com.wflow.service.OrgRepositoryService;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.HttpDefinition;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.props.ConditionProps;
import com.wflow.workflow.bean.process.props.DelayProps;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.execute.ElExecute;
import com.wflow.workflow.execute.HttpExecute;
import com.wflow.workflow.execute.JsExecute;
import com.wflow.workflow.service.UserDeptOrLeaderService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author : JoinFyc
 * @date : 2024/7/15
 */
@Slf4j
@Component("uelTools")
public class UELTools {

    @Autowired
    private UserDeptOrLeaderService userDeptOrLeaderService;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    /**
     * 判断集合是否包含某元素
     *
     * @param collection 集合
     * @param val        目标元素
     * @return 匹配结果
     */
    public boolean contains(Collection<?> collection, Object val) {
        return CollectionUtil.contains(collection, val);
    }

    //判断,集合是否包含某元素
    public boolean contains(String list, Object val) {
        return ArrayUtil.contains(list.split(","), val);
    }

    /**
     * 判断用户是否属于某些用户或者部门内
     *
     * @param orgId        用户/部门ID
     * @param userAndDepts 比较的
     * @return 结果
     */
    public boolean orgContains(String orgId, List<OrgUser> userAndDepts) {
        List<String> users = userAndDepts.stream().filter(v -> "user".equals(v.getType()))
                .map(OrgUser::getId).collect(Collectors.toList());
        if (users.contains(orgId)) {
            return true;
        }
        List<String> collect = userAndDepts.stream().filter(v -> "dept".equals(v.getType()))
                .map(OrgUser::getId).collect(Collectors.toList());
        for (String dept : collect) {
            if (userDeptOrLeaderService.userIsBelongToDept(orgId, dept)) {
                return true;
            }
        }
        return false;
    }



    /**
     * 动态获取延时节点延时时长
     *
     * @param execution 上下文
     * @return 延时表达式
     */
    public String getDelayDuration(ExecutionEntity execution) {
        try {
            Map variable = execution.getVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS, Map.class);
            DelayProps props = (DelayProps) variable.get(execution.getActivityId());
            String date = null;
            if (DelayProps.Type.AUTO.equals(props.getType())){
                date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE) + "T" + props.getDateTime().trim();
            } else if (DelayProps.Type.PRECISE.equals(props.getType())) {
                date = LocalDateTime.parse(props.getDateTime().trim(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .format(DateTimeFormatter.ISO_DATE_TIME);
            }
            return date;
        } catch (Exception e) {
            return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    /**
     * 条件表达式判断
     *
     * @param execution 上下文
     * @return 对比结果
     */
    public boolean conditionCompare(String conditionId, ExecutionEntity execution) {
        Map variable = execution.getVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS, Map.class);
        ConditionProps props = (ConditionProps) variable.get(conditionId);
        Map<String, Object> variables = getContextVar(execution);
        variables.put("root", variables.get(WflowGlobalVarDef.INITIATOR));
        return conditionCompare(props, variables);
    }

    /**
     * 校验条件公共函数
     * @param props 条件设置
     * @param ctx 变量
     * @return 校验结果
     */
    public Boolean conditionCompare(ConditionProps props, Map<String, Object> ctx){
        //拿到前端的条件节点props设置项后开始校验条件
        switch (props.getMode()){
            case SIMPLE:
                return validateSimpleCd(props, ctx);
            case UEL:
                return new ElExecute().execute(props.getExpression(), ctx, Boolean.class);
            case JS:
                return new JsExecute(executorService)
                        .execute("compare", "function compare(ctx){ \n" + props.getJs()  + "\n}",
                                Boolean.class, ctx);
            case HTTP:
                return new HttpExecute().execute(BeanUtil.mapToBean(props.getHttp(), HttpDefinition.class, true), executorService, Boolean.class, ctx);
            default: return false;
        }
    }

    /**
     * 校验简单模式条件设置
     * @param ctx 系统变量
     * @param props 条件设置项
     * @return 校验结果
     */
    private Boolean validateSimpleCd(ConditionProps props, Map<String, Object> ctx){
        int groupConditionSuccess = 0;
        for (ConditionProps.Group group : props.getGroups()) {
            int subConditionSuccess = 0;
            for (ConditionProps.Group.Condition condition : group.getConditions()) {
                if (subConditionCompare(condition, ctx.get(condition.getId()))) {
                    subConditionSuccess++;
                    if ("OR".equals(group.getGroupType())) {
                        //或的关系，那么结束循环，组++
                        groupConditionSuccess++;
                        break;
                    }
                    //全部满足条件也结束循环
                    if (subConditionSuccess == group.getConditions().size()) {
                        groupConditionSuccess++;
                    }
                }
            }
            //判断组对比结果
            if (("OR".equals(props.getGroupsType()) && groupConditionSuccess > 0)
                    || groupConditionSuccess == props.getGroups().size()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 子条件校验
     * @param condition 子条件配置
     * @param compareVal 子条件比较值
     * @return 校验结果
     */
    private boolean subConditionCompare(ConditionProps.Group.Condition condition, Object compareVal) {
        try {
            List<Object> values = condition.getValue();
            double val = 0;
            switch (condition.getCompare()) {
                case ">":
                    return toDouble(compareVal) > toDouble(values.get(0));
                case "<":
                    return toDouble(compareVal) < toDouble(values.get(0));
                case ">=":
                    return toDouble(compareVal) >= toDouble(values.get(0));
                case "<=":
                    return toDouble(compareVal) <= toDouble(values.get(0));
                case "=":
                    return compareVal.toString().equals(String.valueOf(values.get(0)));
                case "!=":
                    return !compareVal.toString().equals(String.valueOf(values.get(0)));
                case "B":
                    val = toDouble(compareVal);
                    return toDouble(values.get(0)) < val && val < toDouble(values.get(1));
                case "AB":
                    val = toDouble(compareVal);
                    return toDouble(values.get(0)) <= val && val < toDouble(values.get(1));
                case "BA":
                    val = toDouble(compareVal);
                    return toDouble(values.get(0)) < val && val <= toDouble(values.get(1));
                case "ABA":
                    val = toDouble(compareVal);
                    return toDouble(values.get(0)) <= val && val <= toDouble(values.get(1));
                case "IN":
                    return values.contains(compareVal.toString());
                case "DEPT":
                    if (compareVal instanceof List){
                        List<String> ids = ((List<Map>) compareVal).stream().map(v -> String.valueOf(v.get("id"))).collect(Collectors.toList());
                        List<String> pids = values.stream().map(v -> String.valueOf(((Map)v).get("id"))).collect(Collectors.toList());
                        for (String sid : ids) {
                            boolean result = false;
                            for (String pid : pids) {
                                if (sid.equals(pid) || userDeptOrLeaderService.deptIsBelongToDept(sid, pid)){
                                    result = true;
                                    break;
                                }
                            }
                            if (!result){
                                return false;
                            }
                        }
                    }
                    return true;
                case "ORG":
                    List<OrgUser> orgs = values.stream().map(v -> {
                        if (v instanceof OrgUser) {
                            return (OrgUser) v;
                        } else if (v instanceof Map) {
                            Map<String, Object> valMap = (Map<String, Object>) v;
                            return OrgUser.builder().id(valMap.get("id").toString())
                                    .type(valMap.get("type").toString()).build();
                        }
                        return null;
                    }).collect(Collectors.toList());
                    if (compareVal instanceof List){
                        List<String> ids = ((List<Map>) compareVal).stream().map(v -> String.valueOf(v.get("id"))).collect(Collectors.toList());
                        for (String id : ids) {
                            if (orgContains(id, orgs)){
                                return true;
                            }
                        }
                    }else {
                        return orgContains(String.valueOf(compareVal), orgs);
                    }
                    return false;
            }
        } catch (Exception e) {
            log.error("条件判断异常[{}]", condition);
        }
        return false;
    }

    /**
     * 获取流程实例上下文变量
     * @param execution 上下文
     * @return 流程实例上下文变量
     */
    public Map<String, Object> getContextVar(DelegateExecution execution) {
        Map<String, Object> variables = execution.getVariables();
        return loadCtxVar(execution.getProcessInstanceId(), execution.getProcessDefinitionId(), variables);
    }

    /**
     * 获取流程实例上下文变量
     * @param instanceId 实例ID
     * @param defId 流程定义ID
     * @return 流程实例上下文变量
     */
    public Map<String, Object> getContextVar(String instanceId, String defId) {
        Map<String, Object> variables = runtimeService.getVariables(instanceId);
        return loadCtxVar(instanceId, defId, variables);
    }

    /**
     * 组装流程实例上下文变量
     * @param instanceId 实例ID
     * @param defId 流程定义ID
     * @param variables 流程实例上下文变量
     * @return 流程实例上下文变量
     */
    private Map<String, Object> loadCtxVar(String instanceId, String defId, Map<String, Object> variables) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().processDefinitionId(defId).singleResult();
        variables.put("formName", definition.getName());
        variables.put("instanceId", instanceId);
        //获取流程发起人信息
        String startUser = String.valueOf(variables.get(WflowGlobalVarDef.INITIATOR));
        String startDept = String.valueOf(variables.get(WflowGlobalVarDef.START_DEPT));
        if (startDept.equals("null")){//为了兼容旧数据
            ProcessInstanceOwnerDto ownerDto = (ProcessInstanceOwnerDto) variables.get(WflowGlobalVarDef.OWNER);
            startDept = Objects.nonNull(ownerDto) ? ownerDto.getOwnerDeptId() : null;
        }
        String key = startUser + "_" + startDept;
        Map<String, UserDeptDo> infoMap = orgRepositoryService.getUserDeptInfos(CollectionUtil.newArrayList(key));
        UserDeptDo userDeptDo = infoMap.getOrDefault(key, new UserDeptDo());
        variables.put("owner_id", startUser);
        variables.put("owner_deptId", startDept);
        variables.put("owner_name", userDeptDo.getUserName());
        variables.put("owner_deptName", userDeptDo.getDeptName());
        return variables;
    }

    private double toDouble(Object val){
        return NumberUtil.parseNumber(val.toString()).doubleValue();
    }

    public void sout(String str){
        System.out.println(str);
    }
}

package com.wflow.workflow.config.custom;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wflow.workflow.bean.dto.ProcessInstanceOwnerDto;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.vo.ProcessHandlerParamsVo;
import com.wflow.workflow.config.WflowGlobalVarDef;
import lombok.Setter;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

import java.util.List;
import java.util.Objects;

/**
 * 设置json类型变量，使对象类型字段存储为json格式字符串且不影响之前的数据
 *
 * @author : willian fu
 * @date : 2024/3/11
 */
public class JsonVariableType implements VariableType {
    private static final long serialVersionUID = 1L;

    @Setter
    private static VariableType defaultVariableType;

    @Override
    public String getTypeName() {
        return "wfJson";
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return true;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (valueFields.getName().equals(WflowGlobalVarDef.WFLOW_FORMS) ||
                valueFields.getName().equals(WflowGlobalVarDef.WFLOW_NODE_PROPS)
                || (value instanceof List && valueFields.getName().startsWith("node_"))) {
            //排除指定类型
            defaultVariableType.setValue(value, valueFields);
        } else if (Objects.isNull(value)) {
            valueFields.setTextValue(null);
        } else if (value instanceof String || value instanceof Number
                || value instanceof Boolean || value instanceof Character
                || value instanceof Enum) {
            valueFields.setTextValue(value.toString());
        } else {
            valueFields.setTextValue(JSON.toJSONString(value));
        }
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        if (WflowGlobalVarDef.WFLOW_FORMS.equals(valueFields.getName()) ||
                WflowGlobalVarDef.WFLOW_NODE_PROPS.equals(valueFields.getName())
                || valueFields.getName().startsWith("node_")) {
            return defaultVariableType.getValue(valueFields);
        } else if (valueFields.getName().startsWith(WflowGlobalVarDef.TASK_RES_PRE) && StrUtil.isNotBlank(valueFields.getTextValue())) {
            //将审批操作类型转换为对象
            return ProcessHandlerParamsVo.Action.valueOf(valueFields.getTextValue());
        } else if (WflowGlobalVarDef.OWNER.equals(valueFields.getName())) {
            //将审批发起人信息类型转换为对象
            return JSONObject.parseObject(valueFields.getTextValue(), ProcessInstanceOwnerDto.class);
        }
        String textValue = valueFields.getTextValue();
        if (JSON.isValid(textValue)) {
            return JSON.parse(textValue);
        }
        return textValue;
    }

    //备用方法，如果流程变量OrgUser类型取人转换报错，就用这个做为转换方法
    private List<?> coverOrgUser(ValueFields valueFields) {
        if (valueFields.getName().startsWith("node_")) {
            if (JSON.isValidArray(valueFields.getTextValue())) {
                JSONArray array = JSONArray.parseArray(valueFields.getTextValue());
                if (array.size() > 0) {
                    JSONObject object = array.getJSONObject(0);
                    if (Objects.nonNull(object) && Objects.nonNull(object.get("id"))) {
                        return array.toJavaList(OrgUser.class);
                    } else {
                        return array.toJavaList(String.class);
                    }
                }
            }
        }
        return null;
    }
}

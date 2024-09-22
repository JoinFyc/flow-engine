package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.wflow.bean.entity.WflowFormRecord;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.service.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 从流程变量存取表单数据
 *
 * @author : JoinFyc
 * @date : 2024/8/24
 */
@Primary
@Service("varFormService")
public class VarFormServiceImpl extends AbstractFormServiceImpl implements FormService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Override
    public Map<String, Object> getProcessInstanceFormData(String instanceId) {
        try {
            return historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(instanceId).variableNameLike("field%").list()
                    .stream().collect(Collectors.toMap(
                            HistoricVariableInstance::getVariableName,
                            HistoricVariableInstance::getValue)
                    );
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getFormFieldData(String instanceId, Collection<String> fieldIds) {
        Map<String, Object> formData;
        //先从运行时变量中获取,没有再从历史变量中获取
        try {
            formData = runtimeService.getVariables(instanceId, fieldIds);
        } catch (Exception e) {
            formData = historyService.createNativeHistoricVariableInstanceQuery()
                    .sql(StrUtil.builder().append("select * from ACT_HI_VARINST where PROC_INST_ID_ = #{instanceId} and TENANT_ID_ = #{tenantId} and NAME_ in ('")
                            .append(String.join("','", fieldIds)).append("')").toString())
                    .parameter("instanceId", instanceId)
                    .parameter("tenantId", UserUtil.getTenantId())
                    .list()
                    .stream().collect(Collectors.toMap(
                            HistoricVariableInstance::getVariableName,
                            HistoricVariableInstance::getValue)
                    );
        }
        return formData;
    }

    @Override
    public Map<String, Map<String, Object>> getFormFieldDataBatch(Map<String, ? extends Collection<String>> instanceFieldMap) {
        //实例id -> (字段id -> 字段值)
        Map<String, Map<String, Object>> instanceDatas = new HashMap<>(instanceFieldMap.size());
        //TODO 暂时循环查，对于流程变量里面查目前没想到更好的办法
        instanceFieldMap.forEach((instanceId, fieldIds) -> {
            instanceDatas.put(instanceId, getFormFieldData(instanceId, fieldIds));
        });
        return instanceDatas;
    }

    @Override
    public void saveInstanceFormData(String instanceId, Map<String, Object> formData) {
        //runtimeService.setVariables(instanceId, formData);
    }

    @Override
    public List<WflowFormRecord> updateInstanceFormData(String userId, String instanceId, Map<String, Object> formData) {
        Map<String, Object> dataMap = new HashMap<>();
        Map<String, Object> variables = runtimeService.getVariables(instanceId, formData.keySet());
        Date time = GregorianCalendar.getInstance().getTime();
        List<WflowFormRecord> records = new ArrayList<>(formData.size());
        formData.forEach((k, newVal) -> {
            Object oldVal = variables.get(k);
            //做下比较，不同就加入修改
            if (isDiff(oldVal, newVal)) {
                dataMap.put(k, newVal);
                records.add(WflowFormRecord.builder()
                        .updateBy(userId).id(IdUtil.objectId())
                        .createTime(time)
                        .fieldId(k).instanceId(instanceId)
                        .oldValue(coverToString(oldVal))
                        .newValue(coverToString(newVal)).build());
            }
        });
        if (CollectionUtil.isNotEmpty(dataMap)) {
            runtimeService.setVariables(instanceId, dataMap);
        }
        return records;
    }
}

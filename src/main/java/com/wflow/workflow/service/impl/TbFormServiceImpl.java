package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowFormData;
import com.wflow.bean.entity.WflowFormRecord;
import com.wflow.bean.entity.WflowModels;
import com.wflow.mapper.WflowFormDataMapper;
import com.wflow.mapper.WflowModelsMapper;
import com.wflow.workflow.bean.process.form.Form;
import com.wflow.workflow.bean.vo.FormAbstractsVo;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.service.FormService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 从 wflow_form_data 表存取表单数据
 *
 * @author : willian fu
 * @date : 2024/2/22
 */
@Service("tbFormService")
public class TbFormServiceImpl extends AbstractFormServiceImpl implements FormService {

    @Autowired
    private WflowFormDataMapper formDataMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private WflowModelsMapper modelsMapper;

    @Override
    public Map<String, Object> getProcessInstanceFormData(String instanceId) {
        Map<String, Object> formData = new HashMap<>();
        formDataMapper.selectList(new LambdaQueryWrapper<WflowFormData>()
                        .eq(WflowFormData::getInstanceId, instanceId))
                .forEach(data -> {
                    String fieldValue = data.getFieldValue();
                    formData.put(data.getFieldName(), data.getIsJson() && fieldValue != null ? JSON.parseObject(fieldValue) : fieldValue);
                });
        return formData;
    }

    @Override
    public Map<String, Object> getFormFieldData(String instanceId, Collection<String> fieldIds) {
        Map<String, Object> formData = new HashMap<>(fieldIds.size());
        formDataMapper.selectList(new LambdaQueryWrapper<WflowFormData>()
                        .eq(WflowFormData::getInstanceId, instanceId)
                        .in(WflowFormData::getFieldName, fieldIds))
                .forEach(data -> {
                    String fieldValue = data.getFieldValue();
                    formData.put(data.getFieldName(), data.getIsJson() && fieldValue != null ? JSON.parseObject(fieldValue) : fieldValue);
                });
        return formData;
    }

    @Override
    public Map<String, Map<String, Object>> getFormFieldDataBatch(Map<String, ? extends Collection<String>> instanceFieldMap) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        //一次性批量查询出来
        Set<String> abstIds = instanceFieldMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        if (CollectionUtil.isEmpty(abstIds)) {
            return result;
        }
        List<WflowFormData> instFormDatas = formDataMapper.selectList(new LambdaQueryWrapper<WflowFormData>()
                .in(WflowFormData::getInstanceId, instanceFieldMap.keySet())
                .in(WflowFormData::getFieldId, abstIds));
        //将结果安装result进行分离
        instFormDatas.forEach(data -> {
            Map<String, Object> formData = result.computeIfAbsent(data.getInstanceId(), k -> new HashMap<>());
            String fieldValue = data.getFieldValue();
            formData.put(data.getFieldId(), data.getIsJson() && fieldValue != null ? JSON.parse(fieldValue) : fieldValue);
        });
        return result;
    }

    @Override
    public void saveInstanceFormData(String instanceId, Map<String, Object> formData) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        //提取表单字段信息
        WflowModels models = modelsMapper.selectOne(new LambdaQueryWrapper<WflowModels>()
                .select(WflowModels::getFormItems)
                .eq(WflowModels::getProcessDefId, instance.getProcessDefinitionId()));
        Map<String, Form> formMap = loadFormItemsMap(models.getFormItems());
        List<WflowFormData> collect = formData.entrySet().stream()
                .filter(v -> null != v.getValue() && formMap.containsKey(v.getKey())).map(entry -> {
            Form field = formMap.getOrDefault(entry.getKey(), new Form());
            //存储表单数据
            return WflowFormData.builder()
                    .id(IdUtil.objectId())
                    .instanceId(instanceId)
                    .defId(instance.getProcessDefinitionId())
                    .createTime(instance.getStartTime())
                    .updateTime(instance.getStartTime())
                    .code(instance.getProcessDefinitionKey())
                    .fieldId(entry.getKey())
                    .fieldKey(field.getKey())
                    .fieldName(field.getTitle())
                    .fieldType(field.getName())
                    .fieldValue(coverToString(entry.getValue()))
                    .isJson(!isSimpleType(entry.getValue()))
                    .build();
        }).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(collect)) {
            if (DbType.MYSQL.equals(WflowGlobalVarDef.DB_TYPE)){
                formDataMapper.insertBatch(collect);
            }else if (DbType.ORACLE.equals(WflowGlobalVarDef.DB_TYPE)){
                formDataMapper.insertOracleBatch(collect);
            }
        }
    }

    @Override
    public List<WflowFormRecord> updateInstanceFormData(String userId, String instanceId, Map<String, Object> formData) {
        List<WflowFormRecord> records = new ArrayList<>(formData.size());
        Date time = GregorianCalendar.getInstance().getTime();
        //先查询出来所有的表单字段
        Set<String> hasKeys = formDataMapper.selectList(new LambdaQueryWrapper<WflowFormData>()
                        .select(WflowFormData::getFieldId).eq(WflowFormData::getInstanceId, instanceId)
                        .in(WflowFormData::getFieldId, formData.keySet()))
                .stream().map(WflowFormData::getFieldId).collect(Collectors.toSet());
        //把新增的字段和需要更新的字段数据分成2个map集合
        Map<String, Object> addData = new HashMap<>();
        Map<String, Object> updateData = new HashMap<>();
        formData.forEach((id, val) -> (hasKeys.contains(id) ? updateData : addData).put(id, val));
        if (CollectionUtil.isNotEmpty(addData)){
            saveInstanceFormData(instanceId, addData);
            addData.forEach((id, val) -> records.add(WflowFormRecord.builder()
                    .fieldId(id).instanceId(instanceId)
                    .oldValue(null)
                    .updateBy(userId)
                    .id(IdUtil.objectId())
                    .createTime(time)
                    .newValue(coverToString(val)).build()));
        }
        if (CollectionUtil.isNotEmpty(updateData)) {
            formData.forEach((id, val) -> {
                WflowFormData data = new WflowFormData();
                data.setInstanceId(instanceId);
                data.setFieldId(id);
                data.setFieldValue(coverToString(val));
                formDataMapper.update(data, new LambdaQueryWrapper<WflowFormData>()
                        .eq(WflowFormData::getInstanceId, instanceId)
                        .eq(WflowFormData::getFieldId, id));
                records.add(WflowFormRecord.builder()
                        .fieldId(id).instanceId(instanceId)
                        .oldValue(coverToString(val))
                        .updateBy(userId)
                        .id(IdUtil.objectId())
                        .createTime(time)
                        .newValue(coverToString(val)).build());
            });
        }
        return records;
    }
}

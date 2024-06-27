package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowFormData;
import com.wflow.bean.entity.WflowFormRecord;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.mapper.WflowFormDataMapper;
import com.wflow.mapper.WflowFormRecordMapper;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.service.OrgRepositoryService;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.form.Form;
import com.wflow.workflow.bean.vo.FormAbstractsVo;
import com.wflow.workflow.bean.vo.FormDataChangeLogVo;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2024/2/23
 */
public abstract class AbstractFormServiceImpl implements FormService {

    @Autowired
    protected WflowFormRecordMapper formRecordMapper;

    @Autowired
    protected WflowFormDataMapper formDataMapper;

    @Autowired
    protected OrgRepositoryService orgRepositoryService;

    @Autowired
    protected WflowModelHistorysMapper modelsMapper;

    @Override
    public void deleteFormData(String instanceId) {
        formDataMapper.delete(new LambdaQueryWrapper<WflowFormData>().eq(WflowFormData::getInstanceId, instanceId));
        formRecordMapper.delete(new LambdaQueryWrapper<WflowFormRecord>().eq(WflowFormRecord::getInstanceId, instanceId));
    }

    @Override
    public List<FormDataChangeLogVo> getFormDataChangeLog(String instanceId, String fieldId) {
        //查询wflow_form_record表，获取instanceId和fieldId对应的记录
        List<WflowFormRecord> formRecords = formRecordMapper.selectList(new LambdaQueryWrapper<WflowFormRecord>()
                .eq(WflowFormRecord::getInstanceId, instanceId)
                .eq(WflowFormRecord::getFieldId, fieldId)
                .orderByDesc(WflowFormRecord::getCreateTime));
        //把formRecords转换成List<FormDataChangeLogVo>返回
        Map<String, OrgUser> userMap = orgRepositoryService.getUsersBatchMap(formRecords.stream()
                .map(WflowFormRecord::getUpdateBy).collect(Collectors.toList()));
        return formRecords.stream().map(record -> {
            OrgUser user = userMap.getOrDefault(record.getUpdateBy(), new OrgUser());
            return FormDataChangeLogVo.builder()
                    .id(record.getFieldId())
                    .oldValue(record.getOldValue())
                    .newValue(record.getNewValue())
                    .updateBy(user)
                    .createTime(record.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, List<FormAbstractsVo>> getInstanceAbstractDatas(Map<String, String> instances) {
        if (CollectionUtil.isEmpty(instances)) {
            return Collections.emptyMap();
        }
        //流程定义ID -> 表单摘要配置信息
        Map<String, List<FormAbstractsVo>> defMap = modelsMapper.selectList(new LambdaQueryWrapper<WflowModelHistorys>()
                        .select(WflowModelHistorys::getFormAbstracts, WflowModelHistorys::getProcessDefId)
                        .in(WflowModelHistorys::getProcessDefId, new HashSet<>(instances.values())))
                //将对应流程定义ID与其对应表单摘要配置取到
                .stream().filter(v -> null != v.getFormAbstracts()).collect(Collectors.toMap(WflowModelHistorys::getProcessDefId,
                        v -> JSONArray.parseArray(v.getFormAbstracts(), FormAbstractsVo.class)));
        //获取流程数据查询参数 实例ID -> 表单字段ID集合
        Map<String, List<String>> listMap = instances.entrySet().stream()
                .filter(v -> defMap.containsKey(v.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        v -> defMap.get(v.getValue()).stream()
                                .map(FormAbstractsVo::getId).collect(Collectors.toList())
                ));
        //查询流程实例的表单摘要数据
        return this.getFormFieldDataBatch(listMap).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                v -> {
                    //当前实例表单摘要配置信息
                    List<FormAbstractsVo> tempList = defMap.get(instances.get(v.getKey()));
                    if (null == tempList) {
                        return Collections.emptyList();
                    }
                    //防止对象混用，重新构建一个新的对象
                    Map<String, Object> values = v.getValue();
                    return tempList.stream().map(item -> FormAbstractsVo.builder()
                            .id(item.getId())
                            .value(values.get(item.getId()))
                            .name(item.getName())
                            .type(item.getType())
                            .build()).collect(Collectors.toList());
                }));

    }

    /**
     * 保存表单字段修改记录
     *
     * @param records 记录
     */
    public int doSaveRecord(List<WflowFormRecord> records) {
        Date time = GregorianCalendar.getInstance().getTime();
        String userId = UserUtil.getLoginUserId();
        records.forEach(record -> {
            record.setId(IdUtil.objectId());
            record.setCreateTime(time);
            record.setUpdateBy(userId);
        });
        if (DbType.MYSQL.equals(WflowGlobalVarDef.DB_TYPE)){
            return formRecordMapper.insertBatch(records);
        }else if (DbType.ORACLE.equals(WflowGlobalVarDef.DB_TYPE)){
            return formRecordMapper.insertOracleBatch(records);
        }
        return 0;
    }

    /**
     * 加载表单配置为map
     *
     * @param formItems 表单配置
     * @return 表单map
     */
    public Map<String, Form> loadFormItemsMap(String formItems) {
        Map<String, Form> formMap = new HashMap<>();
        JSONArray.parseArray(formItems, Form.class).forEach(item -> {
            loadFormItems(formMap, item);
        });
        return formMap;
    }

    //递归加载表单配置
    private void loadFormItems(Map<String, Form> itemsMap, Form item) {
        if ("SpanLayout".equals(item.getName())) {
            JSONObject.from(item.getProps()).getJSONArray("items")
                    .toJavaList(Form.class).forEach(it -> loadFormItems(itemsMap, it));
        } else {
            itemsMap.put(item.getId(), item);
        }
    }

    protected boolean isSimpleType(Object value) {
        return null == value || value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    protected boolean isDiff(Object oldVal, Object newVal) {
        if (isSimpleType(oldVal)) {
            return !Objects.equals(String.valueOf(oldVal), String.valueOf(newVal));
        }
        return !JSON.toJSONString(oldVal).equals(JSON.toJSONString(newVal));
    }

    protected String coverToString(Object val) {
        if (isSimpleType(val)) {
            return String.valueOf(val);
        }
        return JSON.toJSONString(val);
    }
}

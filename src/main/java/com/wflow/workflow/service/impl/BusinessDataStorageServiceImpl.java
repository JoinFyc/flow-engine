package com.wflow.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.wflow.bean.entity.WflowFormRecord;
import com.wflow.workflow.bean.process.form.Form;
import com.wflow.workflow.bean.vo.FormAbstractsVo;
import com.wflow.workflow.bean.vo.FormDataChangeLogVo;
import com.wflow.workflow.service.BusinessDataStorageService;
import com.wflow.workflow.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务数据存储服务，包括表单和流程等一些数据的存储和查询
 *
 * @author : JoinFyc
 * @date : 2023/10/27
 */
@Service
public class BusinessDataStorageServiceImpl implements BusinessDataStorageService {

    //表单数据存储方式
    @Value("${wflow.store.form:all}")
    private String storeForm;

    @Autowired
    @Qualifier("varFormService")
    private FormService varFormService;

    @Autowired
    @Qualifier("tbFormService")
    private FormService tbFormService;

    @Override
    public Map<String, Object> getProcessInstanceFormData(String instanceId) {
        if ("table".equals(storeForm)) {
            return tbFormService.getProcessInstanceFormData(instanceId);
        } else {
            return varFormService.getProcessInstanceFormData(instanceId);
        }
    }

    @Override
    public void deleteFormData(String instanceId) {
        tbFormService.deleteFormData(instanceId);
    }

    @Override
    public List<FormDataChangeLogVo> getFormDataChangeLog(String instanceId, String fieldId) {
        //查询记录的话就不分是哪个实现类了，都是独立的表查
        return varFormService.getFormDataChangeLog(instanceId, fieldId);
    }

    @Override
    public Map<String, Object> getFormFieldData(String instanceId, Collection<String> fieldIds) {
        //只要不是只存流程变量，就只查物理表
        if ("table".equals(storeForm)) {
            return tbFormService.getFormFieldData(instanceId, fieldIds);
        } else {
            return varFormService.getFormFieldData(instanceId, fieldIds);
        }
    }

    @Override
    public Map<String, Map<String, Object>> getFormFieldDataBatch(Map<String,  ? extends Collection<String>> instanceFieldMap) {
        if ("table".equals(storeForm)) {
            return tbFormService.getFormFieldDataBatch(instanceFieldMap);
        } else {
            return varFormService.getFormFieldDataBatch(instanceFieldMap);
        }
    }

    @Override
    public void saveInstanceFormData(String instanceId, Map<String, Object> formData) {
        if ("all".equals(storeForm)) {
            varFormService.saveInstanceFormData(instanceId, formData);
            tbFormService.saveInstanceFormData(instanceId, formData);
        } else if ("var".equals(storeForm)) {
            varFormService.saveInstanceFormData(instanceId, formData);
        } else {
            tbFormService.saveInstanceFormData(instanceId, formData);
        }
    }

    @Override
    public List<WflowFormRecord> updateInstanceFormData(String userId, String instanceId, Map<String, Object> formData) {
        List<WflowFormRecord> records;
        if ("all".equals(storeForm)) {
            records = varFormService.updateInstanceFormData(userId, instanceId, formData);
            tbFormService.updateInstanceFormData(userId, instanceId, formData);
        } else if ("var".equals(storeForm)) {
            records = varFormService.updateInstanceFormData(userId, instanceId, formData);
        } else {
            records = tbFormService.updateInstanceFormData(userId, instanceId, formData);
        }
        if (CollectionUtil.isNotEmpty(records)){
            //添加修改记录
            doSaveRecord(records);
        }
        return records;
    }

    @Override
    public Map<String, Form> loadFormItemsMap(String formItems) {
        return varFormService.loadFormItemsMap(formItems);
    }

    @Override
    public Map<String, List<FormAbstractsVo>> getInstanceAbstractDatas(Map<String, String> instances) {
        return tbFormService.getInstanceAbstractDatas(instances);
    }

    @Override
    public int doSaveRecord(List<WflowFormRecord> records) {
        if ("table".equals(storeForm)) {
            return tbFormService.doSaveRecord(records);
        } else {
            return varFormService.doSaveRecord(records);
        }
    }
}

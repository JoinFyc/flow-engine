package com.wflow.workflow.service;

import com.alibaba.fastjson2.JSONArray;
import com.wflow.bean.entity.WflowFormRecord;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.form.Form;
import com.wflow.workflow.bean.process.form.FormPerm;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.bean.process.props.CcProps;
import com.wflow.workflow.bean.process.props.RootProps;
import com.wflow.workflow.bean.process.props.SubProcessProps;
import com.wflow.workflow.bean.vo.FormAbstractsVo;
import com.wflow.workflow.bean.vo.FormDataChangeLogVo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2022/8/24
 */

public interface FormService {

    /**
     * 通过流程实例获取所有的表单数据
     * @param instanceId 实例ID
     * @return 表单map数据
     */
    Map<String, Object> getProcessInstanceFormData(String instanceId);

    /**
     * 删除流程实例表单数据
     * @param instanceId 实例ID
     */
    void deleteFormData(String instanceId);
    /**
     * 通过流程实例获取指定表单字段修改记录
     * @param instanceId 实例ID
     * @param fieldId 字段ID
     * @return 表单字段值修改记录
     */
    List<FormDataChangeLogVo> getFormDataChangeLog(String instanceId, String fieldId);

    /**
     * 通过流程实例获取表单指定字段数据
     * @param instanceId 实例ID
     * @param fieldIds 字段id集合
     * @return 表单字段数据值
     */
    Map<String, Object> getFormFieldData(String instanceId, Collection<String> fieldIds);

    /**
     * 通过流程实例集合批量获取表单指定流程字段数据，查询流程摘要信息使用
     * @param instanceFieldMap 实例ID和字段ID数据
     */
    Map<String, Map<String, Object>> getFormFieldDataBatch(Map<String, ? extends Collection<String>> instanceFieldMap);

    /**
     * 获取发起人节点的表单渲染配置
     * @param forms 全量表单
     * @param rootProps 发起人节点配置
     * @return 发起人表单配置
     */
    default List<Form> filterFormByPermConfigForRoot(List<Form> forms, RootProps rootProps){
        Map<String, FormPerm.PermEnum> permEnumMap = rootProps.getFormPerms().stream().collect(Collectors.toMap(FormPerm::getId, FormPerm::getPerm));
        return doFilterForm(forms, null, permEnumMap, FormPerm.PermEnum.E);
    }

    /**
     * 保存流程实例表单数据
     * @param instanceId 实例ID
     * @param formData 表单数据
     */
    void saveInstanceFormData(String instanceId, Map<String, Object> formData);

    /**
     * 更新流程实例表单数据
     * @param instanceId 实例ID
     * @param formData 表单数据
     */
    List<WflowFormRecord> updateInstanceFormData(String userId, String instanceId, Map<String, Object> formData);

    /**
     * 加载表单权限，隐藏项将被过滤
     * @param forms 表单配置
     * @param formData 表单数据
     * @param currentNode 节点配置
     * @return 所有只读表单
     */
    default List<Form> filterFormAndDataByPermConfig(List<Form> forms, Map<String, Object> formData, ProcessNode<?> currentNode) {
        FormPerm.PermEnum defaultPerm = FormPerm.PermEnum.R;
        List<FormPerm> formPerms = Collections.emptyList();
        if (Objects.isNull(currentNode)) {
            return doFilterForm(forms, formData, Collections.emptyMap(), defaultPerm);
        }
        switch (currentNode.getType()) {
            case ROOT:
                formPerms = ((RootProps) currentNode.getProps()).getFormPerms();
                defaultPerm = FormPerm.PermEnum.E;
                break;
            case TASK:
            case APPROVAL:
                formPerms = ((ApprovalProps) currentNode.getProps()).getFormPerms();
                break;
            case SUBPROC:
                formPerms = ((SubProcessProps) currentNode.getProps()).getFormPerms();
                break;
            case CC:
                formPerms = ((CcProps) currentNode.getProps()).getFormPerms();
                break;
        }
        Map<String, FormPerm.PermEnum> permEnumMap = formPerms.stream().collect(Collectors.toMap(FormPerm::getId, FormPerm::getPerm));
        return doFilterForm(forms, formData, permEnumMap, defaultPerm);
    }

    /**
     * 过滤表单配置
     * @param forms 表单配置
     * @param formData 表单数据
     * @param permEnumMap 表单权限配置
     * @param defaultPerm 默认权限
     * @return 过滤后的表单配置
     */
    default List<Form> doFilterForm(List<Form> forms, Map<String, Object> formData,
                                    Map<String, FormPerm.PermEnum> permEnumMap, FormPerm.PermEnum defaultPerm) {
        return forms.stream().filter(form -> {
            if ("SpanLayout".equals(form.getName())) {
                form.setPerm(Objects.isNull(formData) ? FormPerm.PermEnum.E : FormPerm.PermEnum.R);
                JSONArray items = (JSONArray) form.getProps().get("items");
                form.getProps().put("items", doFilterForm(items.toJavaList(Form.class), formData, permEnumMap, defaultPerm));
            } else if ("TableList".equals(form.getName())) {
                FormPerm.PermEnum formPerm = permEnumMap.getOrDefault(form.getId(), defaultPerm);
                if (FormPerm.PermEnum.H.equals(formPerm)) {
                    //去除对应数据
                    Optional.ofNullable(formData).ifPresent(data -> data.remove(form.getId()));
                    return false;
                }
                form.setPerm(formPerm);
                JSONArray items = (JSONArray) form.getProps().get("columns");
                form.getProps().put("columns", doFilterForm(items.toJavaList(Form.class), formData, permEnumMap, defaultPerm));
            } else {
                FormPerm.PermEnum formPerm = permEnumMap.getOrDefault(form.getId(), defaultPerm);
                if (FormPerm.PermEnum.H.equals(formPerm)) {
                    //去除对应数据
                    Optional.ofNullable(formData).ifPresent(data -> data.remove(form.getId()));
                    return false;
                }
                form.setPerm(formPerm);
            }
            return true;
        }).collect(Collectors.toList());
    }

    /**
     * 加载表单配置为map
     * @param formItems 表单配置
     * @return 表单map
     */
    Map<String, Form> loadFormItemsMap(String formItems);

    /**
     * 获取表单摘要数据
     * @param instances 流程实例ID -> 流程定义ID
     * @return 表单摘要数据
     */
    Map<String, List<FormAbstractsVo>> getInstanceAbstractDatas(Map<String, String> instances);

    /**
     * 保存表单字段修改记录
     * @param records 记录
     */
    int doSaveRecord(List<WflowFormRecord> records);
}

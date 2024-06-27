package com.wflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowSubGroups;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.bean.vo.WflowSubModelVo;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.WflowSubGroupsMapper;
import com.wflow.mapper.WflowSubProcessMapper;
import com.wflow.service.SubModelGroupService;
import com.wflow.workflow.WFlowToBpmnCreator;
import com.wflow.workflow.bean.process.ProcessNode;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2023/11/26
 */
@Slf4j
@Service
public class SubModelGroupServiceImpl implements SubModelGroupService {


    @Autowired
    private WflowSubGroupsMapper subGroupsMapper;

    @Autowired
    private WflowSubProcessMapper subProcessMapper;

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public List<WflowSubGroups> getGroups() {
        return subGroupsMapper.selectList(
                new LambdaQueryWrapper<WflowSubGroups>()
                        .orderByAsc(WflowSubGroups::getSort));
    }

    @Override
    public List<WflowSubProcess> getModels() {
        return subProcessMapper.getModelList();
    }

    @Override
    public void addGroup(String name) {
        Long count = subGroupsMapper.selectCount(
                new LambdaQueryWrapper<WflowSubGroups>()
                        .eq(WflowSubGroups::getGroupName, name));
        if (count > 0) {
            throw new BusinessException("分组名称不能重复");
        }
        Date time = GregorianCalendar.getInstance().getTime();
        subGroupsMapper.insert(WflowSubGroups.builder()
                .groupName(name).sort(0).created(time).updated(time).build());
    }

    @Override
    public void deleteGroup(Long id) {
        if (id.equals(0) || id.equals(1)){
            throw new BusinessException("不允许删除系统内置分组");
        }
        Long count = subProcessMapper.selectCount(
                new LambdaQueryWrapper<WflowSubProcess>()
                        .eq(WflowSubProcess::getGroupId, id));
        if (count > 0) {
            throw new BusinessException("请先删除分组内的流程");
        }
        subGroupsMapper.deleteById(id);
    }

    @Override
    public void updateGroup(WflowSubGroups group) {
        subGroupsMapper.updateById(group);
    }

    @Override
    public void groupSort(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            if (!ids.get(i).equals(1)) {
                subGroupsMapper.updateById(WflowSubGroups.builder().groupId(ids.get(i)).sort(i).build());
            }
        }
    }

    @Override
    public WflowSubModelVo getModelDetail(String code) {
        WflowSubProcess model = subProcessMapper.getLastVerModel(code);
        if (Objects.nonNull(model)){
            return WflowSubModelVo.builder()
                    .formId(model.getProcCode())
                    .formName(model.getProcName())
                    .processDefId(model.getProcDefId())
                    .groupId(model.getGroupId())
                    .process(model.getProcess())
                    .remark(model.getRemark())
                    .build();
        }
        throw new BusinessException("未找到对应流程数据");
    }

    @Override
    public void deployModel(String code) {
        WflowSubProcess process = subProcessMapper.getLastVerModel(code);
        if (ObjectUtil.isNull(process)) {
            log.warn("子流程id={}不存在", code);
            throw new BusinessException("不存在该子流程");
        }
        ProcessNode<?> processNode = JSONObject.parseObject(process.getProcess(), ProcessNode.class);
        BpmnModel bpmnModel = new WFlowToBpmnCreator().loadBpmnFlowXmlByProcess(process.getProcCode(), process.getProcName(), processNode, true);
        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
        ProcessValidator defaultProcessValidator = processValidatorFactory.createDefaultProcessValidator();
        // 验证失败信息的封装ValidationError
        List<ValidationError> validate = defaultProcessValidator.validate(bpmnModel);
        if (CollectionUtil.isNotEmpty(validate)) {
            log.error("子流程[{}验证失败]：{}", process.getProcCode(), JSONObject.toJSONString(validate));
            throw new BusinessException("子流程设计错误:" + validate.stream()
                    .map(err -> (err.getActivityId() + ":" + err.getActivityName()))
                    .collect(Collectors.joining(",")));
        }
        String xmlString = new String(new BpmnXMLConverter().convertToXML(bpmnModel));
        //  流程部署
        log.debug("流程生成bpmn-xml为：{}", xmlString);
        Deployment deploy = repositoryService.createDeployment()
                .key(process.getProcCode())
                .name(process.getProcName())
                .tenantId("default")
                .addString(process.getProcCode() + ".bpmn", xmlString)
                .deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();
        subProcessMapper.updateById(WflowSubProcess.builder()
                .id(process.getId())
                .deployId(processDefinition.getDeploymentId())
                .procDefId(processDefinition.getId())
                .build());
    }

    @Override
    public String saveModel(WflowSubModelVo modelVo) {
        WflowSubProcess process = modelVo.cover();
        //判断有没有code，没有就新增新版本，有就判断是否发布过，没发布就更新
        if (StrUtil.isNotBlank(process.getProcCode())) {
            WflowSubProcess subProcess = subProcessMapper.getLastVerModel(process.getProcCode());
            if (StrUtil.isBlank(subProcess.getProcDefId())) {
                //没发布过，更新
                process.setId(subProcess.getId());
                subProcessMapper.updateById(process);
                return process.getProcCode();
            } else {
                //发布过，创建新版本
                process.setSort(subProcess.getSort());
                process.setProcCode(subProcess.getProcCode());
                process.setVersion(subProcess.getVersion() + 1);
            }
        } else {
            //没有code，表示新增流程
            process.setProcCode("wfs" + IdUtil.objectId());
            process.setSort(0);
            process.setVersion(1);
        }
        Date time = GregorianCalendar.getInstance().getTime();
        process.setId(IdUtil.objectId());
        process.setCreated(time);
        process.setUpdated(time);
        process.setIsDeleted(false);
        process.setIsStop(false);
        subProcessMapper.insert(process);
        return process.getProcCode();
    }

    @Override
    public void processSort(Long groupId, List<String> ids) {
        for (int i = 0; i < ids.size(); i++) {
            subProcessMapper.update(
                    WflowSubProcess.builder().sort(i).build(),
                    new LambdaQueryWrapper<WflowSubProcess>()
                            .eq(WflowSubProcess::getId, ids.get(i))
                            .eq(WflowSubProcess::getGroupId, groupId));
        }
    }

    @Override
    public void enableProcess(String id, Long groupId, boolean enable) {
        WflowSubProcess model = subProcessMapper.selectById(id);
        if (enable){
            subProcessMapper.updateById(WflowSubProcess.builder()
                    .id(model.getId()).isStop(false).groupId(groupId).build());
        }else {
            subProcessMapper.updateById(WflowSubProcess.builder()
                    .id(model.getId()).isStop(true).groupId(0L).build());
        }
    }

    @Override
    public void modelMoveToGroup(String id, Long groupId) {
        subProcessMapper.updateById(WflowSubProcess.builder().id(id).groupId(groupId).build());
    }


}

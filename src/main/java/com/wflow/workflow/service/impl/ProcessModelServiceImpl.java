package com.wflow.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wflow.bean.FlowProcessContext;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowModelPerms;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.vo.CustomPrintConfigVo;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.mapper.WflowModelPermsMapper;
import com.wflow.mapper.WflowModelsMapper;
import com.wflow.service.OrgRepositoryService;
import com.wflow.utils.RpcUtil;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.WFlowToBpmnCreator;
import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.props.RootProps;
import com.wflow.workflow.bean.vo.FormAbstractsVo;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.service.ProcessModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : JoinFyc
 * @date : 2024/8/25
 */
@Slf4j
@Service
public class ProcessModelServiceImpl implements ProcessModelService {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private WflowModelHistorysMapper modelHistorysMapper;

    @Autowired
    private WflowModelsMapper modelsMapper;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    @Autowired
    private WflowModelPermsMapper modelPermsMapper;

    @Override
    @Transactional
    public String saveProcess(WflowModelHistorys models) {
        //TODO business_event_key校验
        //提取摘要字段，进行保存
        models.setFormAbstracts(JSON.toJSONString(loadFormAbstracts(models.getFormItems())));
        if (ObjectUtil.isNull(models.getFormId())) {
            models.setCreated(GregorianCalendar.getInstance().getTime());
            models.setFormId("wf" + IdUtil.objectId());
            models.setVersion(1);
            WflowModels wflowModels = new WflowModels();
            BeanUtil.copyProperties(models, wflowModels);
            wflowModels.setIsDelete(false);
            wflowModels.setIsStop(false);
            wflowModels.setUpdated(new Date());
            wflowModels.setSort(0);
            //TODO 模型表&模型历史表 --add column business_event_key
            //TODO 新增业务事件表&事件规则表&事件接口表&事件接口字段表....
            modelHistorysMapper.insert(models);
            //初始的流程在部署表也存一份，用来查询
            modelsMapper.insert(wflowModels);
            log.info("保存流程[{}]初始V1新版本", models.getFormId());
        } else {
            //检查最新版本有没有部署过
            WflowModelHistorys lastVersionModel = getLastVersionModel(models.getFormId());
            if (StrUtil.isNotBlank(lastVersionModel.getProcessDefId())) {
                //发布过，构建新版
                models.setProcessDefId(null);
                models.setVersion(lastVersionModel.getVersion() + 1);
                models.setCreated(GregorianCalendar.getInstance().getTime());
                modelHistorysMapper.insert(models);
                log.info("保存流程[{}]的新版本，版本号:{}", lastVersionModel.getFormId(), models.getVersion());
            } else {
                //没发布，就覆盖
                models.setId(lastVersionModel.getId());
                models.setCreated(null);
                models.setVersion(null);
                modelHistorysMapper.updateById(models);
                log.info("保存未发布的流程[{}]的，版本号:{}", lastVersionModel.getFormId(), lastVersionModel.getVersion());
            }
        }
        return models.getFormId();
    }

    @Override
    @Transactional
    public void enableProcess(String code, boolean enable) {
        WflowModels wflowModels = modelsMapper.selectById(code);
        modelsMapper.updateById(WflowModels.builder().formId(code).isStop(enable).build());
        try {
            if (enable){
                repositoryService.activateProcessDefinitionById(wflowModels.getProcessDefId());
            }else {
                repositoryService.suspendProcessDefinitionById(wflowModels.getProcessDefId());
            }
        } catch (Exception e) {
            log.warn("流程[{}]没有发布过，无需操作", code);
        }
    }

    @Override
    @Transactional
    public String deployProcess(String code) {
        //从历史表获取最新版本的流程，然后放到发布表再部署
        WflowModelHistorys wflowModels = getLastVersionModel(code);
        if (ObjectUtil.isNull(wflowModels)) {
            log.warn("流程{}不存在", code);
            throw new BusinessException("不存在该表单");
        }
        ProcessNode<?> processNode = JSONObject.parseObject(wflowModels.getProcess(), ProcessNode.class);
        //TODO 流程转换器&流程校验器，单例模型
        //构建流程模型，并验证
        BpmnModel bpmnModel = new WFlowToBpmnCreator().loadBpmnFlowXmlByProcess(wflowModels.getFormId(), wflowModels.getFormName(), processNode, false);
        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
        ProcessValidator defaultProcessValidator = processValidatorFactory.createDefaultProcessValidator();
        // 验证失败信息的封装ValidationError
        List<ValidationError> validate = defaultProcessValidator.validate(bpmnModel);
        if (CollectionUtil.isNotEmpty(validate)) {
            log.error("流程[{}验证失败]：{}", code, JSONObject.toJSONString(validate));
            throw new BusinessException("bpmn生成错误:" + validate.stream()
                    .map(err -> (err.getActivityName() + ":" + err.getProblem()))
                    .collect(Collectors.joining(",")));
        }
        String xmlString = new String(new BpmnXMLConverter().convertToXML(bpmnModel));
        //  流程部署 TODO 部署校验：例如条件网关设置错误，也能通过部署。但是流程实例生成会报错。排查问题，全部打印到日志
        log.info("流程生成bpmn-xml为：{}", xmlString);
        Deployment deploy = repositoryService.createDeployment()
                .key(code)
                .name(wflowModels.getFormName())
                .tenantId(UserUtil.getTenant().getTenantId()) //TODO 租户改造
                .category(String.valueOf(wflowModels.getGroupId()))
                .addString(wflowModels.getFormId() + ".bpmn", xmlString)
                .deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();
        WflowModels models = new WflowModels();
        BeanUtil.copyProperties(wflowModels, models);
        models.setProcessDefId(processDefinition.getId());
        models.setVersion(processDefinition.getVersion()); //部署的版本号，流程定义的版本号: v=v+1
        models.setIsDelete(false);
        models.setIsStop(false);
        models.setDeployId(deploy.getId());
        models.setUpdated(new Date());
        models.setSort(0);
        modelsMapper.updateById(models);
        modelHistorysMapper.updateById(WflowModelHistorys.builder()
                .version(processDefinition.getVersion())
                .deployId(deploy.getId())
                .processDefId(processDefinition.getId())
                .id(wflowModels.getId()).build());
        //解析配置的权限，进行拆分 TODO 表单的权限控制
        reloadModelsPerm(code, processNode);
        //部署流程就是从历史表提取最新的流程
        log.info("部署流程{}成功，ID={}:", code, deploy.getId());
        return deploy.getId();
    }

    /**
     * 解析表单发起权限控制设置
     *
     * @param code        表单流程编号
     * @param processNode 节点数据
     */
    private void reloadModelsPerm(String code, ProcessNode<?> processNode) {
        //清空之前的权限然后重新设置
        modelPermsMapper.delete(new LambdaQueryWrapper<WflowModelPerms>()
                .eq(WflowModelPerms::getFormId, code));
        List<OrgUser> assignedUser = ((RootProps) processNode.getProps()).getAssignedUser();
        if (CollectionUtil.isNotEmpty(assignedUser)) {
            Date time = GregorianCalendar.getInstance().getTime();
            //开始解析所有选择的部门的子部门，深度遍历
            Set<String> deptSet = new HashSet<>();
            Set<String> userSet = new HashSet<>();
            assignedUser.forEach(org -> {
                if ("dept".equals(org.getType())) {
                    deptSet.add(org.getId());
                    deptSet.addAll(orgRepositoryService.getRecursiveSubDept(org.getId()));
                } else {
                    userSet.add(org.getId());
                }
            });
            List<WflowModelPerms> modelPerms = deptSet.stream()
                    .map(d -> WflowModelPerms.builder()
                            .id(IdUtil.objectId())
                            .formId(code)
                            .orgId(d)
                            .permType("dept")
                            .createTime(time)
                            .build()).collect(Collectors.toList());
            modelPerms.addAll(userSet.stream()
                    .map(u -> WflowModelPerms.builder()
                            .id(IdUtil.objectId())
                            .formId(code)
                            .orgId(u)
                            .permType("user")
                            .createTime(time)
                            .build()).collect(Collectors.toList()));
            if (DbType.MYSQL.equals(WflowGlobalVarDef.DB_TYPE)){
                modelPermsMapper.insertBatch(modelPerms);
            }else if (DbType.ORACLE.equals(WflowGlobalVarDef.DB_TYPE)){
                modelPermsMapper.insertOracleBatch(modelPerms);
            }
        }
    }

    @Override
    @Transactional
    public void delProcess(String code) {
        enableProcess(code, false);
        modelsMapper.deleteById(code);
    }

    @Override
    public WflowModelHistorys getLastVersionModel(String code) {
        Page<WflowModelHistorys> historys = modelHistorysMapper.selectPage(new Page<>(1,1),
                new LambdaQueryWrapper<WflowModelHistorys>()
                        .eq(WflowModelHistorys::getFormId, code)
                        .orderByDesc(WflowModelHistorys::getVersion));
        if (CollectionUtil.isNotEmpty(historys.getRecords())) {
            return historys.getRecords().get(0);
        }
        throw new BusinessException("未找到该流程");
    }

    @Override
    public CustomPrintConfigVo getCustomPrintConfig(String instanceId) {
        try {
            HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceId).singleResult();
            WflowModelHistorys modelHistory = modelHistorysMapper.selectOne(
                    new LambdaQueryWrapper<WflowModelHistorys>()
                            .select(WflowModelHistorys::getSettings)
                            .eq(WflowModelHistorys::getProcessDefId, instance.getProcessDefinitionId())
                            .eq(WflowModelHistorys::getVersion, instance.getProcessDefinitionVersion()));
            return JSONObject.parseObject(modelHistory.getSettings(), CustomPrintConfigVo.class);
        } catch (Exception e) {
            return new CustomPrintConfigVo();
        }
    }

    @Override
    public WflowModelHistorys getModelByDefId(String defId) {
        return modelHistorysMapper.selectOne(
                new LambdaQueryWrapper<WflowModelHistorys>()
                        .eq(WflowModelHistorys::getProcessDefId, defId));
    }


    private List<FormAbstractsVo> loadFormAbstracts(String formItems){
        ArrayList<FormAbstractsVo> abstractsVos = new ArrayList<>();
        if (StringUtils.isNotBlank(formItems)) {
            return abstractsVos;
        }
        JSONArray.parseArray(formItems, JSONObject.class).forEach(item -> {
            loadFormItems(abstractsVos, item);
        });
        return abstractsVos;
    }

    private void loadFormItems(List<FormAbstractsVo> formItems, JSONObject item){
        JSONObject props = item.getJSONObject("props");
        if("SpanLayout".equals(item.getString("name"))){
            props.getJSONArray("items").forEach(it -> {
                loadFormItems(formItems, (JSONObject) it);
            });
        } else if (Boolean.TRUE.equals(props.getBoolean("abstract"))){
            formItems.add(FormAbstractsVo.builder()
                    .id(item.getString("id"))
                    .name(item.getString("title"))
                    .type(item.getString("name"))
                    .build());
        }
    }
}

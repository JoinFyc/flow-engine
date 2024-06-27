package com.wflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowModelGroups;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.vo.ModelGroupVo;
import com.wflow.bean.vo.WflowModelDetailVo;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.WflowModelGroupsMapper;
import com.wflow.mapper.WflowModelsMapper;
import com.wflow.service.ModelGroupService;
import com.wflow.service.OrgRepositoryService;
import com.wflow.workflow.bean.process.ProcessNode;
import com.wflow.workflow.bean.process.form.Form;
import com.wflow.workflow.bean.process.props.RootProps;
import com.wflow.workflow.service.FormService;
import com.wflow.workflow.service.ProcessModelService;
import com.wflow.workflow.service.ProcessNodeCatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @date : 2022/7/4
 */
@Slf4j
@Service
@Transactional
public class FormGroupServiceImpl implements ModelGroupService {

    @Autowired
    private WflowModelGroupsMapper groupsMapper;

    @Autowired
    private WflowModelsMapper modelsMapper;

    @Autowired
    private ProcessModelService modelService;

    @Autowired
    private ProcessNodeCatchService nodeCatchService;

    @Autowired
    private FormService formService;

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    @Override
    public List<ModelGroupVo> getGroupModels(String userId, String modelName) {
        List<ModelGroupVo> modelGroupVos = new LinkedList<>();
        //一次性查出所有表单，再分组归类
        Map<Long, List<ModelGroupVo.Form>> groupMap = new LinkedHashMap<>();
        List<ModelGroupVo.Form> models = Objects.nonNull(userId) ?
                orgRepositoryService.getModelsByPerm(userId) :
                modelsMapper.getSysModels();
        models.forEach(m -> {
            List<ModelGroupVo.Form> forms = groupMap.get(m.getGroupId());
            if (Objects.isNull(forms)){
                forms = new ArrayList<>();
                groupMap.put(m.getGroupId(), forms);
            }
            forms.add(m);
        });
        groupsMapper.selectList(new LambdaQueryWrapper<WflowModelGroups>()
                .orderByAsc(WflowModelGroups::getSort)).forEach(group -> {
            ModelGroupVo modelGroupVo = ModelGroupVo.builder()
                    .id(group.getGroupId())
                    .name(group.getGroupName())
                    .items(new LinkedList<>())
                    .build();
            modelGroupVos.add(modelGroupVo);
            modelGroupVo.setItems(groupMap.getOrDefault(group.getGroupId(), Collections.emptyList()));
        });
        return modelGroupVos;
    }

    @Override
    public void modelGroupsSort(List<Long> groups) {
        List<Long> list = groups.stream().filter(id -> !id.equals(0L) && !id.equals(1L)).collect(Collectors.toList());
        for (int i = 0; i < list.size(); i++) {
            groupsMapper.updateById(WflowModelGroups.builder().groupId(list.get(i)).sort(i).build());
        }
    }

    @Override
    public Object getModelById(String formId) {
        WflowModels wflowModels = modelsMapper.selectById(formId);
        ProcessNode<?> root = nodeCatchService.reloadProcessByStr(wflowModels.getProcess()).get("root");
        List<Form> forms = formService.filterFormByPermConfigForRoot(JSONArray.parseArray(wflowModels.getFormItems(), Form.class), (RootProps) root.getProps());
        return WflowModelDetailVo.builder()
                .formId(formId).formItems(forms)
                .formName(wflowModels.getFormName())
                .logo(wflowModels.getLogo())
                .formConfig(JSONObject.parseObject(wflowModels.getFormConfig()))
                .processDefId(wflowModels.getProcessDefId())
                .process(root)
                .build();
    }

    @Override
    public Object getModelByDefId(String defId) {
        WflowModels models = modelsMapper.selectOne(new LambdaQueryWrapper<WflowModels>()
                .eq(WflowModels::getProcessDefId, defId));
        if (Objects.isNull(models)) {
            throw new BusinessException("该流程已被重新发布，原有版本失效");
        }
        return models;
    }

    @Override
    public void updateModelGroupName(Long id, String name) {
        groupsMapper.updateById(WflowModelGroups.builder().groupId(id).groupName(name).build());
    }

    @Override
    public void createModelGroup(String name) {
        groupsMapper.insert(WflowModelGroups.builder().sort(0).groupName(name).updated(new Date()).build());
    }

    @Override
    @Transactional
    public void deleteModelGroup(Long id) {
        //先转移，再删除分组
        if (Objects.nonNull(id) && id > 1) {
            Set<String> collect = modelsMapper.selectList(new LambdaQueryWrapper<WflowModels>()
                            .select(WflowModels::getFormId)
                            .eq(WflowModels::getGroupId, id)).stream()
                    .map(WflowModels::getFormId).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(collect)) {
                //移动到其他分组 id = 1
                modelsMapper.update(WflowModels.builder().groupId(1L).build(),
                        new LambdaQueryWrapper<WflowModels>()
                                .in(WflowModels::getFormId, collect));
            }
            groupsMapper.deleteById(id);
        } else {
            throw new BusinessException("系统默认分组不允许删除");
        }
    }

    @Override
    public void groupModelSort(Long groupId, List<String> modelIds) {
        WflowModels wflowModels = WflowModels.builder().groupId(groupId).build();
        for (int i = 0; i < modelIds.size(); i++) {
            wflowModels.setFormId(modelIds.get(i));
            wflowModels.setSort(i);
            modelsMapper.updateById(wflowModels);
        }
    }

    @Override
    @Transactional
    public void deleteModel(String modelId) {
        modelService.delProcess(modelId);
        modelsMapper.updateById(WflowModels.builder().formId(modelId).isDelete(true).build());
    }

    @Override
    public void modelMoveToGroup(String modelId, Long groupId) {
        modelsMapper.updateById(WflowModels.builder().formId(modelId).groupId(groupId).sort(0).build());
    }

    @Override
    public void enOrDisModel(String modelId, Boolean active) {
        modelsMapper.updateById(WflowModels.builder().formId(modelId)
                .groupId(Boolean.TRUE.equals(active) ? 0 : 1L)
                .isStop(Boolean.TRUE.equals(active)).build());
    }

    @Override
    public List<WflowModels> getModelItem() {
        return modelsMapper.selectList(new LambdaQueryWrapper<WflowModels>()
                .select(WflowModels::getFormId, WflowModels::getFormName)
                .eq(WflowModels::getIsStop, false));
    }

    @Override
    public List<WflowModelGroups> getModelGroups() {
        return groupsMapper.selectList(new LambdaQueryWrapper<WflowModelGroups>()
                .notIn(WflowModelGroups::getGroupId, CollectionUtil.newArrayList(0, 1))
                .orderByAsc(WflowModelGroups::getSort));
    }

}

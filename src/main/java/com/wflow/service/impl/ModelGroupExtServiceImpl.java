package com.wflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowModelGroups;
import com.wflow.bean.vo.ModelGroupVo;
import com.wflow.mapper.WflowModelGroupsMapper;
import com.wflow.mapper.WflowModelsMapper;
import com.wflow.service.ModelGroupExtService;
import com.wflow.service.OrgRepositoryService;
import com.wflow.workflow.service.FormService;
import com.wflow.workflow.service.ProcessModelService;
import com.wflow.workflow.service.ProcessNodeCacheService;
import jakarta.annotation.Resource;

import java.util.*;

/**
 * @author JoinFyc
 * @description 表单分类
 * @date 2024-08-26
 */
public class ModelGroupExtServiceImpl implements ModelGroupExtService {

    @Resource
    private WflowModelGroupsMapper groupsMapper;

    @Resource
    private WflowModelsMapper modelsMapper;

    @Resource
    private ProcessModelService modelService;

    @Resource
    private ProcessNodeCacheService nodeCatchService;

    @Resource
    private FormService formService;

    @Resource
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
}

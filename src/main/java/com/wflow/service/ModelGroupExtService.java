package com.wflow.service;

import com.wflow.bean.entity.WflowModelGroups;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.vo.ModelGroupVo;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/7/4
 */
public interface ModelGroupExtService {


    List<ModelGroupVo> getGroupModels(String userId, String modelName);

}

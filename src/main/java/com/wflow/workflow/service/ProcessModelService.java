package com.wflow.workflow.service;

import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.vo.CustomPrintConfigVo;

/**
 * @author : willian fu
 * @date : 2022/8/25
 */
public interface ProcessModelService {

    String saveProcess(WflowModelHistorys models);

    void enableProcess(String code, boolean enable);

    String deployProcess(String code);

    void delProcess(String code);

    WflowModelHistorys getLastVersionModel(String code);

    CustomPrintConfigVo getCustomPrintConfig(String instanceId);

    WflowModelHistorys getModelByDefId(String defId);
}

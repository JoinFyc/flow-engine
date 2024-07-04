package com.wflow.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowModels;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @since : 2024/6/14
 */
@Slf4j
@SpringBootTest
public class FlowTest {

    @Resource
    private WflowNotifysMapper notifysMapper;
    @Resource
    private WflowCcTasksMapper ccTasksMapper;
    @Resource
    private WflowModelsMapper modelsMapper;
    @Resource
    private WflowModelHistorysMapper historysMapper;
    @Resource
    private WflowSubProcessMapper subProcessMapper;
    @Resource
    private WflowUserAgentsMapper agentsMapper;
    @Resource
    private WflowModelPermsMapper permsMapper;
    @Resource
    private WflowFormDataMapper formDataMapper;
    @Resource
    private WflowFormRecordMapper formRecordMapper;

    /**
     * 清除wflow环境数据，保留最新版本设计
     */
    @Test
    public void cleanWflowData(){
        //清空抄送表
        ccTasksMapper.delete(new QueryWrapper<>());
        //清空代理人设置
        agentsMapper.delete(new QueryWrapper<>());
        //清空表单数据及记录
        formDataMapper.delete(new QueryWrapper<>());
        formRecordMapper.delete(new QueryWrapper<>());
        //清空通知消息
        notifysMapper.delete(new QueryWrapper<>());
        //清除权限设置
        permsMapper.delete(new QueryWrapper<>());
        //清除历史版本记录表，只保留最大版本号的流程
        historysMapper.delete(new QueryWrapper<>());
        //查询最新版本流程，然后重置版本号，放回his表
        modelsMapper.update(WflowModels.builder().isDelete(false).isStop(false).version(1).build(), null);
        List<WflowModelHistorys> historys = modelsMapper.selectList(new QueryWrapper<>()).stream().map(v -> {
            WflowModelHistorys modelHistory = new WflowModelHistorys();
            BeanUtils.copyProperties(v, modelHistory);
            modelHistory.setDeployId(null);
            modelHistory.setProcessDefId(null);
            return modelHistory;
        }).collect(Collectors.toList());
        historys.forEach(his -> historysMapper.insert(his));
        //处理子流程
        List<WflowSubProcess> modelList = subProcessMapper.getModelList();
        //找到需要保留的
        List<String> collect = modelList.stream().map(WflowSubProcess::getId).collect(Collectors.toList());
        subProcessMapper.delete(new LambdaQueryWrapper<WflowSubProcess>().notIn(WflowSubProcess::getId, collect));
        subProcessMapper.update(WflowSubProcess.builder().version(1).build(), new LambdaQueryWrapper<WflowSubProcess>()
                .in(WflowSubProcess::getId, collect));

    }
}

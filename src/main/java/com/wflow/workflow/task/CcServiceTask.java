package com.wflow.workflow.task;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowCcTasks;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowSubProcess;
import com.wflow.mapper.WflowCcTasksMapper;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.mapper.WflowSubProcessMapper;
import com.wflow.utils.BeanUtil;
import com.wflow.workflow.bean.dto.NotifyDto;
import com.wflow.workflow.config.WflowGlobalVarDef;
import com.wflow.workflow.service.NotifyService;
import com.wflow.workflow.service.ProcessTaskService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 抄送任务
 *
 * @author : willian fu
 * @date : 2022/7/15
 */
@Slf4j
public class CcServiceTask implements JavaDelegate {

    private static WflowCcTasksMapper ccTasksMapper;

    private static ProcessTaskService processTaskService;

    private static NotifyService notifyService;

    private static WflowSubProcessMapper subProcessMapper;

    private static WflowModelHistorysMapper historysMapper;

    public CcServiceTask() {
        ccTasksMapper = BeanUtil.getBean(WflowCcTasksMapper.class);
        processTaskService = BeanUtil.getBean(ProcessTaskService.class);
        notifyService = BeanUtil.getBean(NotifyService.class);
        subProcessMapper = BeanUtil.getBean(WflowSubProcessMapper.class);
        historysMapper = BeanUtil.getBean(WflowModelHistorysMapper.class);
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            FlowElement element = execution.getCurrentFlowElement();
            //异步执行抄送
            Set<String> ccUsers = processTaskService.getCcTaskUsers(execution.getProcessInstanceId(), element.getId());
            if (CollectionUtil.isNotEmpty(ccUsers)) {
                //批量数据入库，创建抄送任务
                Date time = GregorianCalendar.getInstance().getTime();
                String parentId = execution.getRootProcessInstanceId();
                final String code, formName;
                if (StrUtil.isNotBlank(parentId) && !parentId.equals(execution.getProcessInstanceId())){
                    //是子流程
                    WflowSubProcess subProcess = subProcessMapper.selectOne(new LambdaQueryWrapper<WflowSubProcess>()
                            .select(WflowSubProcess::getProcName, WflowSubProcess::getProcCode)
                            .eq(WflowSubProcess::getProcDefId, execution.getProcessDefinitionId()));
                    code = subProcess.getProcCode();
                    formName = subProcess.getProcName();
                }else {
                    WflowModelHistorys historys = historysMapper.selectOne(new LambdaQueryWrapper<WflowModelHistorys>()
                            .select(WflowModelHistorys::getFormName, WflowModelHistorys::getFormId)
                            .eq(WflowModelHistorys::getProcessDefId, execution.getProcessDefinitionId()));
                    code = historys.getFormId();
                    formName = historys.getFormName();
                }

                List<WflowCcTasks> ccTasks = ccUsers.stream().map(u -> WflowCcTasks.builder()
                        .instanceId(execution.getProcessInstanceId())
                        .nodeId(element.getId())
                        .code(code)
                        .nodeName(element.getName())
                        .userId(u)
                        .createTime(time)
                        .build()).collect(Collectors.toList());
                int num = 0;
                if (DbType.MYSQL.equals(WflowGlobalVarDef.DB_TYPE)) {
                    num = ccTasksMapper.insertBatch(ccTasks);
                } else if (DbType.ORACLE.equals(WflowGlobalVarDef.DB_TYPE)) {
                    num = ccTasksMapper.insertOracleBatch(ccTasks);
                }
                NotifyDto notifyDto = NotifyDto.builder()
                        .title("审批抄送送通知")
                        .nodeId(element.getId())
                        .processDefId(execution.getProcessDefinitionId())
                        .instanceId(execution.getProcessInstanceId())
                        .content(StrUtil.builder("您收到【" + formName +"】的审批抄送，请知晓").toString())
                        .type(NotifyDto.TypeEnum.INFO)
                        .build();
                ccTasks.forEach(cc -> {
                    //拷贝一下防止重复消息
                    NotifyDto dto = new NotifyDto();
                    BeanUtils.copyProperties(notifyDto, dto);
                    dto.setTarget(cc.getUserId());
                    notifyService.notify(dto);
                });
                log.info("{} 执行抄送 {} {}人", element.getId(), element.getName(), num);
            }
        } catch (Exception e) {
            log.error("[{}]执行抄送[{}]异常", execution.getProcessInstanceId(), execution.getCurrentActivityId());
        }
    }
}

package com.wflow.workflow.config.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowModels;
import com.wflow.mapper.WflowModelsMapper;
import com.wflow.service.MessagePublisher;
import com.wflow.workflow.bean.vo.ProcessHandlerParamsVo;
import com.wflow.workflow.config.WflowGlobalVarDef;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.service.delegate.BaseTaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author JoinFyc
 * @description 最后一个用户任务
 * @date 2024-08-07
 */
@Slf4j
@Component("lastUserTaskEventListener")
public class LastUserTaskEventListener implements TaskListener {

    @Autowired
    private TaskService taskService;

    @Autowired
    private WflowModelsMapper wflowModelsMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void notify(DelegateTask delegateTask) {

        log.info("delegateTask:{},{}", delegateTask.getExecutionId(),delegateTask.getEventName());

        //有事件通知
        ProcessHandlerParamsVo params = delegateTask.getVariable(WflowGlobalVarDef.TASK_EVENT_PRE + delegateTask.getId(), ProcessHandlerParamsVo.class);
        if (params == null) return;

        //最后一个用户任务完成
        if (!Objects.equals(delegateTask.getEventName(), "complete") && taskService.createTaskQuery()
                .processInstanceId(delegateTask.getProcessInstanceId())
                .active()
                .count() == 0) return;

        //节点状态为已同意
        final ProcessHandlerParamsVo.Action action = params.getAction();
        if (action != ProcessHandlerParamsVo.Action.agree) return;

        //通知方式
        final ProcessHandlerParamsVo.Notify notify = params.getNotify();
        if (notify == null) return;
        /**
         * 请求：
         * 用户ID： 取
         * URL：
         * params: 假期天数取表单
         * businessEventKey对应的业务类型： 比如请假类型
         */
        //业务Key组装报文参数 TODO 请求参数的组装，是不是可以写代码，用springEL表达式动态执行，不新增事件请求的字段维护？是不是还可以动态更新代码的执行逻辑，具体动态脚本写在Nacos中？这样只需要编写请求脚本和配置相应的BusinessKey即可
        WflowModels wflowModels = wflowModelsMapper.selectOne(new LambdaQueryWrapper<WflowModels>().eq(WflowModels::getProcessDefId, delegateTask.getProcessDefinitionId()));
        final String businessEventKey = wflowModels.getBusinessEventKey();
        String message = null;

        //TODO 事件通知的方式可自定义切换，前期先HTTP
        switch (notify) {
            case sync_mq_interface:
            case sync_http_interface:
            case sync_redis_interface: {
                //TODO redis配置方式，发布订阅相关配置
                log.info("sync_redis_interface,{}", delegateTask.getId());
                try {
                    redisTemplate.convertAndSend("wflow_channel", message);
                } catch (Exception e) {
                    log.error("sync_redis_interface error{},message={}", e,message);
                }
            }

        }
    }

}

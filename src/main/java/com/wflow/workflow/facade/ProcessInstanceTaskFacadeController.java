package com.wflow.workflow.facade;

import com.wflow.bean.FlowProcessContext;
import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.vo.ProcessHandlerParamsVo;
import com.wflow.workflow.service.ProcessTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * @author JoinFyc
 * @description 用户任务
 * @date 2024-08-29
 */
@RestController
@RequestMapping("/flow-engine/rest/process/task")
public class ProcessInstanceTaskFacadeController {

    @Autowired
    private ProcessTaskService taskService;

    /**
     * 获取所有可回退的审批任务节点
     *
     * @param instanceId 审批实例ID
     * @param taskId     当前任务ID
     * @return 所有可回退节点
     */
    @GetMapping("recall/nodes")
    public Object getRecallTaskNodes(@RequestParam String instanceId,
                                     @RequestParam String taskId) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("获取所有可回退的审批任务节点");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        return R.ok(taskService.getRecallTaskNodes(instanceId, taskId));
    }

}

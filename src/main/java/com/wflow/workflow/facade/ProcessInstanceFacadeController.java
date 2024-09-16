package com.wflow.workflow.facade;

import com.wflow.bean.FlowProcessContext;
import com.wflow.utils.R;
import com.wflow.workflow.bean.vo.ProcessHandlerParamsVo;
import com.wflow.workflow.bean.vo.ProcessStartParamsVo;
import com.wflow.workflow.service.ProcessInstanceService;
import com.wflow.workflow.service.ProcessTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author JoinFyc
 * @description 流程模型
 * @date 2024-08-27
 */
@Tag(name = "表单处理", description = "表单处理相关接口")
@RestController
@RequestMapping("/flow-engine/rest/instance")
public class ProcessInstanceFacadeController {

    @Autowired
    private ProcessInstanceService processService;

    @Autowired
    private ProcessTaskService taskService;

    /**
     * 发起审批流程
     * @param defId  流程定义ID
     * @param params 参数
     * @return 操作结果
     */
    @PostMapping("start")
    @Operation(summary = "发起流程")
    public Object startProcess(@RequestParam String defId,
                               @RequestBody ProcessStartParamsVo params) {

        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("发起流程-");
        String instanceId = processService.startProcess(defId, params);
        return R.ok("启动流程实例 " + instanceId + " 成功");
    }

    /**
     * 获取系统内发起的流程-- 我发起的
     *
     * @param pageSize 每页数量
     * @param pageNo   页码
     * @param formId     表单流程ID（流程定义KEY）
     * @param finished 流程是否已经结束
     * @return 列表数据
     */
    @GetMapping("submittedList")
    @Operation(summary = "我发起的")
    public Object getUserSubmittedList(@RequestParam(defaultValue = "20") Integer pageSize,
                                       @RequestParam(defaultValue = "1") Integer pageNo,
                                       @RequestParam(required = false) String formId,
                                       @RequestParam(required = false) String[] startTimes,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Boolean finished,
                                       @RequestParam(required = false) String fieldId,
                                       @RequestParam(required = false) String fieldVal) {

        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("不取表单摘要和头像数据");
        return R.ok(processService.getUserSubmittedList(pageSize, pageNo, flowProcessContext.getUserId(), formId,
                finished, startTimes, keyword, fieldId, fieldVal));
    }

    /**
     * 获取抄送我的事项
     *
     * @param pageSize 每页数量
     * @param pageNo   页码
     * @param formId     表单模型ID，流程定义KEY
     * @return 超送我的审批实例
     */
    @GetMapping("ccMe")
    @Operation(summary = "抄送我的")
    public Object getCcMeInstance(@RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(defaultValue = "1") Integer pageNo,
                                  @RequestParam(required = false) String[] startTimes,
                                  @RequestParam(required = false) String formId
    ) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("抄送我-不取表单摘要和头像数据");
        return R.ok(processService.getCcMeInstance(pageSize, pageNo, formId, startTimes));
    }

    /**
     * 查询用户待办待处理的任务
     *
     * @param pageSize   每页条数
     * @param pageNo     页码
     * @param formId       流程定义key，模型ID
     * @param startTimes 任务开始时间范围
     * @param keyword    关键字(任务节点名称、流程名称)
     * @return 分页列表数据
     */
    @GetMapping("todoList")
    @Operation(summary = "待我处理")
    public Object getUserTodoList(@RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(defaultValue = "1") Integer pageNo,
                                  @RequestParam(required = false) String formId,
                                  @RequestParam(required = false) String[] startTimes,
                                  @RequestParam(required = false) String keyword) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("我审批-不取表单摘要和头像数据");
        return R.ok(taskService.getUserTodoList(pageSize, pageNo, formId, startTimes, keyword));
    }


    /**
     * 查询流程表单数据及审批的进度步骤
     * "instanceId": "wf202408081055337317613",
     * "taskDefKey": "node_855834905380",
     *
     * @param instanceId 流程实例ID
     * @param nodeId 当前获取流程人员关联的流程节点ID
     * @return 流程进度及表单详情
     */
    @GetMapping("progress")
    @Operation(summary = "流程详情-进程")
    public Object getProcessFormAndInstanceProgress(@RequestParam String instanceId,@RequestParam(required = false) String nodeId) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("查询审批进度-取当前用户ID判断是否允许撤销");
        return R.ok(processService.getInstanceProgress(nodeId, instanceId));
    }

    /**
     * 用户处理任务，审批、转交、评论、撤销等操作
     *
     * @param params 操作参数
     * @return 操作结果
     */
    @PostMapping("handler")
    @Operation(summary = "流程处理")
    public Object approvalTask(@RequestBody ProcessHandlerParamsVo params) {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("用户处理任务，审批、转交、评论、撤销等操作");
        taskService.approvalTask(flowProcessContext.getUserId(), flowProcessContext.getUserId(), params);
        return R.ok("处理成功");
    }


    /**
     * 查询工作台上方 我发起的、带我处理、关于我的统计数量
     *
     * @return 统计数据
     */
    @GetMapping("count")
    @Operation(summary = "工作台-统计")
    public Object getProcessInstanceCount() {
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldDesc("查询流程模型数据");
        flowProcessContext.setFieldTag(Boolean.TRUE);
        return R.ok(processService.getProcessInstanceCount());
    }

}

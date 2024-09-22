package com.wflow.workflow.controller;

import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.vo.ProcessStartParamsVo;
import com.wflow.workflow.service.ProcessInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author : JoinFyc
 * @date : 2024/8/24
 */
@RestController
@RequestMapping("wflow/process")
public class ProcessInstanceController {

    @Autowired
    private ProcessInstanceService processService;

    /**
     * 查询工作台上方 我发起的、带我处理、关于我的统计数量
     *
     * @return 统计数据
     */
    @GetMapping("instance/count")
    public Object getProcessInstanceCount() {
        return R.ok(processService.getProcessInstanceCount());
    }

    /**
     * 发起审批流程
     *
     * @param defId  流程定义ID
     * @param params 参数
     * @return 操作结果
     */
    @PostMapping("start/{defId}")
    public Object startTheProcess(@PathVariable String defId,
                                  @RequestBody ProcessStartParamsVo params) {
        String instanceId = processService.startProcess(defId, params);
        return R.ok("启动流程实例 " + instanceId + " 成功");
    }

    /**
     * 获取审批流程实例待处理的任务列表
     *
     * @param instanceId 流程实例ID
     * @return 列表数据
     */
    @GetMapping("{instanceId}/taskList")
    public Object getProcessInstanceTaskList(@PathVariable String instanceId) {
        return R.ok(processService.getProcessInstanceTaskList(instanceId));
    }

    /**
     * 获取系统内发起的流程
     *
     * @param pageSize 每页数量
     * @param pageNo   页码
     * @param code     表单流程ID（流程定义KEY）
     * @param finished 流程是否已经结束
     * @return 列表数据
     */
    @GetMapping("mySubmitted")
    public Object getUserSubmittedList(@RequestParam(defaultValue = "20") Integer pageSize,
                                       @RequestParam(defaultValue = "1") Integer pageNo,
                                       @RequestParam(required = false) String code,
                                       @RequestParam(required = false) String[] startTimes,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Boolean finished,
                                       @RequestParam(required = false) String fieldId,
                                       @RequestParam(required = false) String fieldVal) {
        return R.ok(processService.getUserSubmittedList(pageSize, pageNo, UserUtil.getLoginUserId(), code,
                finished, startTimes, keyword, fieldId, fieldVal));
    }

    /**
     * 获取系统中所有已发起的流程实例
     *
     * @param pageSize 每页数量
     * @param pageNo   页码
     * @param code     表单流程ID（流程定义KEY）
     * @param finished 流程是否已经结束
     * @return 列表数据
     */
    @GetMapping("submittedList")
    public Object getSubmittedList(@RequestParam(defaultValue = "20") Integer pageSize,
                                   @RequestParam(defaultValue = "1") Integer pageNo,
                                   @RequestParam(required = false) String code,
                                   @RequestParam(required = false) String[] startTimes,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Boolean finished,
                                   @RequestParam(required = false) String fieldId,
                                   @RequestParam(required = false) String fieldVal) {
        return R.ok(processService.getUserSubmittedList(pageSize, pageNo, null, code,
                finished, startTimes, keyword, fieldId, fieldVal));
    }

    /**
     * 查询流程表单数据及审批的进度步骤
     *
     * @param instanceId 流程实例ID
     * @param nodeId     当前获取流程人员关联的流程节点ID
     * @return 流程进度及表单详情
     */
    @GetMapping("progress/{instanceId}/{nodeId}")
    public Object getProcessFormAndInstanceProgress(@PathVariable String instanceId,
                                                    @PathVariable(required = false) String nodeId) {
        return R.ok(processService.getInstanceProgress(nodeId, instanceId));
    }

    /**
     * 获取抄送我的事项
     *
     * @param pageSize 每页数量
     * @param pageNo   页码
     * @param code     表单模型ID，流程定义KEY
     * @return 超送我的审批实例
     */
    @GetMapping("ccMe")
    public Object getCcMeInstance(@RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(defaultValue = "1") Integer pageNo,
                                  @RequestParam(required = false) String[] startTimes,
                                  @RequestParam(required = false) String code) {
        return R.ok(processService.getCcMeInstance(pageSize, pageNo, code, startTimes));
    }

    /**
     * 删除流程实例
     *
     * @param instanceId 实例ID
     * @return 操作结果
     */
    @DeleteMapping("instance/{instanceId}")
    public Object delProcessInstance(@PathVariable String instanceId) {
        processService.delProcessInstance(instanceId);
        return R.ok("删除流程实例成功");
    }

}

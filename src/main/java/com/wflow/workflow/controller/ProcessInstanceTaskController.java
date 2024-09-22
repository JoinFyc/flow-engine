package com.wflow.workflow.controller;

import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.vo.ProcessHandlerParamsVo;
import com.wflow.workflow.service.ProcessTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * @author : JoinFyc
 * @date : 2024/8/25
 */
@RestController
@RequestMapping("wflow/process/task")
public class ProcessInstanceTaskController {

    @Autowired
    private ProcessTaskService taskService;

    /**
     * 查询用户待办待处理的任务
     *
     * @param pageSize   每页条数
     * @param pageNo     页码
     * @param code       流程定义key，模型ID
     * @param startTimes 任务开始时间范围
     * @param keyword    关键字(任务节点名称、流程名称)
     * @return 分页列表数据
     */
    @GetMapping("todoList")
    public Object getUserTodoList(@RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(defaultValue = "1") Integer pageNo,
                                  @RequestParam(required = false) String code,
                                  @RequestParam(required = false) String[] startTimes,
                                  @RequestParam(required = false) String keyword) {
        return R.ok(taskService.getUserTodoList(pageSize, pageNo, code, startTimes, keyword));
    }

    /**
     * 查询用户已审批的流程实例
     *
     * @param pageSize 每页条数
     * @param pageNo   页码
     * @param code     流程定义key，模型ID
     * @return 分页列表数据
     */
    @GetMapping("idoList")
    public Object getUserIdoList(@RequestParam(defaultValue = "20") Integer pageSize,
                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                 @RequestParam(required = false) String code) {
        return R.ok(taskService.getUserIdoList(pageSize, pageNo, code));
    }

    /**
     * 用户处理任务，审批、转交、评论、撤销等操作
     *
     * @param params 操作参数
     * @return 操作结果
     */
    @PostMapping("handler")
    public Object approvalTask(@RequestBody ProcessHandlerParamsVo params) {
        String userId = UserUtil.getLoginUserId();
        taskService.approvalTask(userId, userId, params);
        return R.ok("处理成功");
    }

    /**
     * 管理员处理任务接口
     *
     * @param owner 该任务原主人ID
     * @param params 操作参数
     * @return 操作结果
     */
    @PostMapping("replace/{owner}/handler")
    public Object approvalTask(@PathVariable String owner, @RequestBody ProcessHandlerParamsVo params) {
        String userId = UserUtil.getLoginUserId();
        taskService.approvalTask(owner, userId, params);
        return R.ok("处理成功");
    }

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
        return R.ok(taskService.getRecallTaskNodes(instanceId, taskId));
    }

    /**
     * 查询节点审批设置项
     *
     * @param taskId 要处理的任务ID
     * @return 该任务所属节点的设置项
     */
    @GetMapping("settings/{taskId}")
    public Object getTaskSettings(@PathVariable String taskId) {
        return R.ok(taskService.getNodeTaskSettings(taskId));
    }

    /**
     * 工作交接设置接口
     *
     * @param userId 交接人
     * @return 交接结果
     */
    @GetMapping("handover/{userId}")
    public Object workHandover(@PathVariable String userId) {
        taskService.workHandover(UserUtil.getLoginUserId(), userId);
        return R.ok("设置工作交接完毕");
    }

}

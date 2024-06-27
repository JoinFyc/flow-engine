package com.wflow.controller;

import com.wflow.utils.R;
import com.wflow.workflow.service.NotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/9/20
 */
@RestController
@RequestMapping("wflow/notify")
public class NotifyController {

    @Autowired
    private NotifyService notifyService;

    /**
     * 查询用户通知消息
     * @param pageSize 每页条数
     * @param pageNo 页码
     * @return 分页数据
     */
    @GetMapping
    public Object getUserNotifys(@RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam(defaultValue = "1") Integer pageNo){
        return R.ok(notifyService.getNotify(pageSize, pageNo));
    }

    /**
     * 批量将消息设置为已读
     * @param ids 消息ID列表
     * @return 操作结果
     */
    @PutMapping
    public Object readNotifys(@RequestBody List<String> ids){
        notifyService.readNotify(ids);
        return R.ok("消息已读");
    }
}

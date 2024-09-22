package com.wflow.workflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wflow.bean.entity.WflowNotifys;
import com.wflow.workflow.bean.dto.NotifyDto;

import java.util.List;
import java.util.Set;

/**
 * @author : JoinFyc
 * @date : 2024/9/17
 */
public interface NotifyService {
    /**
     * 消息通知接口
     * @param notify 通知参数
     */
    void notify(NotifyDto notify);

    /**
     * 分页获取通知消息
     * @param pageSize 分页尺寸
     * @param pageNo 页码
     * @return 通知
     */
    Page<WflowNotifys> getNotify(Integer pageSize, Integer pageNo);

    /**
     * 消息已读
     * @param ids 消息通知ID
     */
    void readNotify(List<String> ids);
}

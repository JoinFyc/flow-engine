package com.wflow.workflow.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.bean.entity.WflowNotifys;
import com.wflow.config.AsyncTaskTheadPoolConfig;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.mapper.WflowNotifysMapper;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.dto.NotifyDto;
import com.wflow.workflow.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

/**
 * 消息通知服务
 *
 * @author : JoinFyc
 * @date : 2024/9/17
 */
@Slf4j
@Service
public class DefaultNotifyServiceImpl implements NotifyService {

    @Autowired
    private WflowNotifysMapper notifysMapper;

    @Autowired
    private WflowModelHistorysMapper modelHistorysMapper;

    /**
     * 发送消息通知到自定义的渠道
     *
     * @param notify      消息实体类
     * @param model       该消息关联的流程模型
     * @param notifySetUp 该流程消息通知设置项
     */
    private void notifyCustom(NotifyDto notify, WflowModelHistorys model, JSONObject notifySetUp) {
        //TODO 大家自定义通知到自己想要的地方
    }

    @Override
    public void notify(NotifyDto notify) {
        AsyncTaskTheadPoolConfig.taskScheduler.submit(() -> {
            WflowModelHistorys model = modelHistorysMapper.selectOne(new LambdaQueryWrapper<WflowModelHistorys>()
                    .select(WflowModelHistorys::getSettings)
                    .eq(WflowModelHistorys::getProcessDefId, notify.getProcessDefId()));
            Optional.ofNullable(model).ifPresent(md -> {
                JSONObject notifySetUp = JSONObject.parseObject(md.getSettings()).getJSONObject("notify");
                notifyCustom(notify, model, notifySetUp);
                //保存通知消息
                notifysMapper.insert(WflowNotifys.builder()
                        .id(IdUtil.objectId())
                        .title(notify.getTitle())
                        .instanceId(notify.getInstanceId())
                        .content(notify.getContent())
                        .userId(notify.getTarget())
                        .link(null)
                        .nodeId(notify.getNodeId())
                        .type(notify.getType())
                        .readed(false)
                        .createTime(null == notify.getCreateTime() ?
                                GregorianCalendar.getInstance().getTime()
                                : notify.getCreateTime())
                        .build());
                //TODO 可以调用websocket实时推送通知
                log.info("推送通知[{}]给[{} 内容为{}]", notify.getTarget(), notify.getTitle(), notify.getContent());
            });
        });
    }

    @Override
    public Page<WflowNotifys> getNotify(Integer pageSize, Integer pageNo) {
        return notifysMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<WflowNotifys>()
                        .eq(WflowNotifys::getReaded, false)
                        .eq(WflowNotifys::getUserId, UserUtil.getLoginUserId())
                        .orderByDesc(WflowNotifys::getCreateTime)
        );
    }

    @Override
    public void readNotify(List<String> ids) {
        notifysMapper.update(WflowNotifys.builder().readed(true).build(),
                new LambdaQueryWrapper<WflowNotifys>().in(WflowNotifys::getId, ids)
                        .eq(WflowNotifys::getUserId, UserUtil.getLoginUserId()));
    }
}

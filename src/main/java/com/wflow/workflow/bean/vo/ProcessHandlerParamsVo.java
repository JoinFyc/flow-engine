package com.wflow.workflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author : willian fu
 * @date : 2022/8/24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessHandlerParamsVo {
    //实例ID
    private String instanceId;
    //任务ID
    private String taskId;
    //意见
    private ProcessComment comment;
    //签名图片地址
    private String signature;
    //是否更新签名
    private Boolean updateSign;
    //操作类型
    private Action action;
    //目标用户
    private String targetUser;
    //目标节点
    private String targetNode;
    //表单数据
    private Map<String, Object> formData;
    //指定其他节点审批人 节点ID->选择的用户ID列表
    private Map<String, List<String>> otherNodeUsers;
    //通知方式
    private Notify notify;
    @Data
    @NoArgsConstructor
    public static class ProcessComment{
        //文字评论
        protected String text;
        //评论附件
        protected List<Attachment> attachments;

        public ProcessComment(String text, List<Attachment> attachments) {
            this.text = text;
            this.attachments = null == attachments ? Collections.emptyList() : attachments;
        }
    }

    @Data
    public static class Attachment{
        private String id;
        //文件名
        private String name;
        //文件类型
        private Boolean isImage;
        //访问地址
        private String url;
    }

    public enum Action{
        //完成，同意、驳回、评论、取消、退回、委派、转交、前加签、后加签
        complete, agree, refuse, comment, cancel, recall, delegate, transfer, beforeAdd, afterAdd;
    }

    public enum Notify {
        //同步，异步，邮件，mq，应用内，redis
        sync_mq_interface,sync_http_interface,sync_redis_interface,
    }
}

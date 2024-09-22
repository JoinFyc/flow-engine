package com.wflow.workflow.bean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/9/17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyDto {
    //被通知的目标 可以是用户或者其他对象
    private String target;
    //标题
    private String title;
    //消息类型
    private TypeEnum type;
    //触发消息的流程节点ID
    private String nodeId;
    //流程实例ID
    private String instanceId;
    //对应的流程定义ID
    private String processDefId;
    //内容
    private String content;
    //点击跳转的链接地址
    private String link;
    //扩展字段数据
    private Map<String, Object> extend;
    //通知产生的时间
    private Date createTime;

    //通知类型
    public enum TypeEnum{
        INFO, SUCCESS, WARNING, ERROR;
    }
}

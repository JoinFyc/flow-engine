package com.wflow.bean.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.wflow.workflow.bean.dto.NotifyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WflowNotifys implements Serializable {
    private static final long serialVersionUID = 992391300778007010L;

    @TableId(type = IdType.INPUT)
    private String id;
    /**
    * 标题
    */
    private String title;
    /**
    * 用户id
    */
    private String userId;
    /**
    * 内容
    */
    private String content;
    /**
     * 消息类型
     */
    private NotifyDto.TypeEnum type;
    //审批实例ID
    private String instanceId;
    /**
    * 是否已读
    */
    private Boolean readed;
    /**
    * 跳转链接
    */
    private String link;
    //节点ID
    private String nodeId;

    private Date createTime;


}

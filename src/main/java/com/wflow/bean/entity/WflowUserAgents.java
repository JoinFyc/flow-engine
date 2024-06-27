package com.wflow.bean.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WflowUserAgents implements Serializable {
    private static final long serialVersionUID = 132789974985360813L;

    @TableId(type = IdType.INPUT)
    /**
    * 用户ID
    */
    private String userId;
    /**
    * 审批代理人ID
    */
    private String agentUserId;
    /**
    * 代理开始日期
    */
    private Date startTime;
    /**
    * 代理结束日期
    */
    private Date endTime;

    private Date createTime;


}

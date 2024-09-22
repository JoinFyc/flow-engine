package com.wflow.workflow.bean.vo;

import com.wflow.workflow.bean.process.OrgUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2024/9/11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentVo extends ProcessHandlerParamsVo.ProcessComment{

    private String id;

    private String type;

    private String taskId;

    private String commentType;

    private OrgUser user;

    private Date createTime;

}

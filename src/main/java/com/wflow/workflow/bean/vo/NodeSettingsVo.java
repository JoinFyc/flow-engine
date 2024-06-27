package com.wflow.workflow.bean.vo;

import com.wflow.workflow.bean.process.OrgUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务处理设置项，在打开任务处理弹框的时候获取
 * @author : willian fu
 * @date : 2023/10/11
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeSettingsVo {

    //处理该任务是否需要签字
    private Boolean enableSign;
    //为其他节点指定审批人
    private List<ApChooseUser> chooseUsers;

    @Data
    @AllArgsConstructor
    public static class ApChooseUser {

        private String nodeId;

        private String nodeName;
        //节点已经指定的审批人
        private List<OrgUser> user;
    }
}

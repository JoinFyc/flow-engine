package com.wflow.bean.vo;

import com.wflow.workflow.bean.process.OrgUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/10/2
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAgentVo {
    //时间范围
    private List<String> timeRange;

    //代理人
    private OrgUser user;

    //是否有效
    private Boolean effective;
}

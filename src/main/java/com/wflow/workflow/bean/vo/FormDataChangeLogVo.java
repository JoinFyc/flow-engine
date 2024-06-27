package com.wflow.workflow.bean.vo;

import com.wflow.workflow.bean.process.OrgUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 表单数据修改记录VO
 * @author : willian fu
 * @date : 2024/2/22
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormDataChangeLogVo {
    private String id;
    // 修改人
    private OrgUser updateBy;
    // 旧的值
    private String oldValue;
    //新的值
    private String newValue;
    // 修改时间
    private Date createTime;
}

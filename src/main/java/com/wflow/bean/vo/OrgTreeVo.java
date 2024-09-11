package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : willian fu
 * @version : 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgTreeVo {
    //部门ID、用户ID
    private String id;
    //部门名、用户名
    private String name;
    //类型，dept部门、user用户
    private String type;
    //是否为本部门负责人
    private Boolean isLeader;
    //用户头像
    private String avatar;
    //冗余字段
    private Boolean sex;
    //冗余字段
    private Boolean selected;
    //公司编号
    private String companyId;
}

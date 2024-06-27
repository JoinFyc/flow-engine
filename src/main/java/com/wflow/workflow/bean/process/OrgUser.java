package com.wflow.workflow.bean.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : willian fu
 * @date : 2022/7/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrgUser implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;
    //部门ID/用户ID
    private String id;
    //部门名/用户名
    private String name;
    //用户头像
    private String avatar;
    //类型，user=用户 dept=部门
    private String type;
    //用户性别
    private Boolean sex;

}

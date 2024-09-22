package com.wflow.bean.do_;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : JoinFyc
 * @date : 2024/08/29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDo {

    private String roleId;
    /**
     * 标签ID
     */
    private String roleName;
}

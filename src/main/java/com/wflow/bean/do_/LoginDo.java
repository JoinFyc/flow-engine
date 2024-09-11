package com.wflow.bean.do_;

import lombok.*;

/**
 * @author JoinFyc
 * @version : 1.0
 * @description rcp-Facade-登录信息
 * @date 2024 09:09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class LoginDo {

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 用户ID
     */
    private String userId;

}

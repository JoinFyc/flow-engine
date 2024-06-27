package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : willian fu
 * @date : 2022/8/16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginVo {

    private String userId;

    private String userName;

    private String alisa;

    private String position;

    private String avatar;

    private String token;

}

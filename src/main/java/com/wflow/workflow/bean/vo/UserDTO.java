package com.wflow.workflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author JoinFyc
 * @version : 1.0
 * @description
 * @date 2024 09:14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private String userName;

    private Long userId;
}
package com.wflow.workflow.bean.vo;

import lombok.*;

import java.util.List;

/**
 * @author JoinFyc
 * @version : 1.0
 * @description 分组排序
 * @date 2024 09:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public class FormGroupVo {

    private List<Long> groups;
}

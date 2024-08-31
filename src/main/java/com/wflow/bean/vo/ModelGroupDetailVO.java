package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author JoinFyc
 * @description 模型分组信息
 * @date 2024-08-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelGroupDetailVO {

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 排序
     */
    private Integer sort;


    /**
     * 更新时间
     */
    private Date updated;
}

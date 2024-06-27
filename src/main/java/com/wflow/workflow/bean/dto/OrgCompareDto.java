package com.wflow.workflow.bean.dto;

import lombok.Data;

import java.util.List;

/**
 * @author : willian fu
 * @date : 2023/8/4
 */
@Data
public class OrgCompareDto {
    //源数据
    private List<String> sourceIds;
    //用来比较的id
    private List<String> targetIds;
}

package com.wflow.bean.vo;

import com.wflow.bean.entity.WflowSubProcess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : JoinFyc
 * @date : 2023/11/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubModelGroupVo {

    private Long id;

    private String name;

    private List<WflowSubProcess> items;
}

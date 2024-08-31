package com.wflow.bean.vo.remote.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author JoinFyc
 * @description 流程模型-快照
 * @date 2024-08-27
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowModelGroupRequest {

    @Schema(description = "表单分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String groupName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建人")
    private String createUser;
}

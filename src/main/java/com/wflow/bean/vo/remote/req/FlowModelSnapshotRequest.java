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
public class FlowModelSnapshotRequest {

    @Schema(description = "表单ID")
    private String formId;

    @Schema(description = "表单名称",requiredMode = Schema.RequiredMode.REQUIRED)
    private String formName;

    @Schema(description = "流程图标")
    private String logo;

    @Schema(description = "扩展设置")
    private String settings;

    @Schema(description = "流程分组ID",requiredMode = Schema.RequiredMode.REQUIRED)
    private String groupId;

    @Schema(description = "表单UI内容")
    private String formItems;

    @Schema(description = "表单规则&表单提交校验")
    private String formConfig;

    @Schema(description = "流程配置")
    private String processConfig;

    @Schema(description = "流程定义",requiredMode = Schema.RequiredMode.REQUIRED)
    private String process;

    @Schema(description = "流程说明")
    private String remark;

    @Schema(description = "业务流程")
    private String businessEventKey;

    @Schema(description = "表单类型")
    private String formType;
}

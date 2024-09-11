package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author : willian fu
 * @date : 2020/9/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelGroupVo {

    private Long id;

    private String name;

    private List<Form> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Form{

        private String formId;

        private String formType;

        private Integer version;

        private String processDefId;

        private String formName;

        private String logo;

        private Long groupId;

        private Boolean isStop;

        private String remark;

        private Date updated;
    }


}

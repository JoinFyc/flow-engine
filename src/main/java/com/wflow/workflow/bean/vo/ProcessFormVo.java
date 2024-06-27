package com.wflow.workflow.bean.vo;

import com.wflow.workflow.bean.process.form.Form;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : willian fu
 * @date : 2022/8/24
 */
@Data
public class ProcessFormVo {

    private List<Form> forms;

    private Map<String, Object> formData;
}

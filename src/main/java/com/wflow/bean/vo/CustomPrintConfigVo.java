package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : willian fu
 * @date : 2023/3/3
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomPrintConfigVo {

    private Boolean customPrint;

    private String printTemplate;
}

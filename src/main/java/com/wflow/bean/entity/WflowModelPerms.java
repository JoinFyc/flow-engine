package com.wflow.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author : willian fu
 * @date : 2022/11/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WflowModelPerms  implements Serializable {
    private static final long serialVersionUID = -80075781855060928L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String formId;

    private String permType;

    private String orgId;

    private Date createTime;
}

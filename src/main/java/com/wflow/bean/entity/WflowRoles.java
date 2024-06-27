package com.wflow.bean.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WflowRoles implements Serializable {
    private static final long serialVersionUID = 685445053985109974L;

    @TableId(type = IdType.AUTO)

    private String roleId;
    /**
    * 标签ID
    */
    private String roleName;
    /**
    * 创建时间
    */
    private Date created;


}

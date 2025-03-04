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
public class WflowDepartments implements Serializable {
    private static final long serialVersionUID = 457951701966938674L;

    @TableId(type = IdType.INPUT)
    /**
    * 部门id
    */
    private String id;
    /**
    * 部门名
    */
    private String deptName;
    /**
    * 部门主管
    */
    private String leader;
    /**
    * 父部门id
    */
    private String parentId;
    /**
    * 创建时间
    */
    private Date created;
    /**
    * 更新时间
    */
    private Date updated;


}

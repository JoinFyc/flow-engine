package com.wflow.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WflowModelGroups implements Serializable {
    private static final long serialVersionUID = -40467384325438214L;

    @TableId(type = IdType.ASSIGN_ID)
    /**
    * 分组ID
    */
    private Long groupId;
    /**
    * 分组名称
    */
    private String groupName;
    /**
    * 分组类型
    */
    private String groupType;
    /**
    * 排序
    */
    private Integer sort;

    /**
    * 更新时间
    */
    private Date updated;
    /**
     * 创建人
     */
    private String createUser;
    /**
     * 状态
     */
    private Integer status;
    /**
    * 创建时间
    */
    private Date created;


}

package com.wflow.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.io.Serializable;

/**
 * 子流程分组表(WflowSubGroups)实体类
 *
 * @author makejava
 * @since 2023-11-27 11:55:27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WflowSubGroups implements Serializable {
    private static final long serialVersionUID = 418056235863568998L;
    /**
     * 分组id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long groupId;
    /**
     * 分组名
     */
    private String groupName;
    /**
     * 排序号
     */
    private Integer sort;
    /**
     * 创建时间
     */
    private Date created;
    /**
     * 更新时间
     */
    private Date updated;

}


package com.wflow.bean.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 职位表
 * @author:  wangzh
 * @created: 2024-05-15 14:30:53
 * ---------------------------------------------------
 * 日期    时间    修改人    修改说明
 * 2024-05-15 14:30:53    wangzh    初始化文件
 * ---------------------------------------------------
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionInfo implements Serializable {

    /**
     * bigint(20) UNSIGNED  职位ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long autoNo;
    /**
     * varchar(50)  职位名称
     */
    private String positionName;
    /**
     * varchar(50)  职位编码
     */
    private String positionCode;
    /**
     * bigint(20)  所属部门ID
     */
    private Long deptNo;
    /**
     * varchar(50)  所属部门名称
     */
    private String deptName;
    /**
     * bigint(20)  职位属性ID(字典)
     */
    private Long attributeId;
    /**
     * varchar(50)  职位属性名称
     */
    private String attributeName;
    /**
     * 起始等级
     */
    private Integer attributeStartNum;
    /**
     * 终点等级
     */
    private Integer attributeEndNum;
    /**
     * bigint(20)  职位层级ID
     */
    private Long positionLevelId;
    /**
     * varchar(50)  职位层级名称
     */
    private String positionLevelName;
    /**
     * bigint(20)  岗位起始薪资ID
     */
    private Long salaryStartId;
    /**
     * decimal(11, 2)  岗位起始薪资
     */
    private BigDecimal salaryStart;
    /**
     * bigint(20)  岗位终点薪资ID
     */
    private Long salaryEndId;
    /**
     * decimal(11, 2)  岗位终点薪资
     */
    private BigDecimal salaryEnd;
    /**
     * bigint(20)  职位级别ID
     */
    private Long positionGradeId;
    /**
     * varchar(50)  职位级别名称
     */
    private String positionGradeName;
    /**
     * int(11)  职位编制人数
     */
    private Integer authorizedNum;
    /**
     * varchar(200)  任职资格
     */
    private String qualifications;
    /**
     * varchar(200)  职位职责
     */
    private String responsibilities;

    /**
     * 性别
     */
    private String sex;
    /**
     * 年龄
     */
    private String age;
    /**
     * 学习
     */
    private String education;
    /**
     *   标签
     */
    private String labelNo;
    /**
     * 职级
     */
    private String levelNo;

    /**
     * varchar(200)  职位说明书
     */
    private String positionDescription;
    /**
     * 排序字段
     */
    private Long orderSequence;
    /**
     * tinyint(2)  是否已删除：0-未删除，1-已删除
     */
    private Integer isDeleted;
    /**
     * tinyint(4) UNSIGNED  启用状态(0:不启用; 1:启用)
     */
    private Integer enabled;
    /**
     * bigint(11)  公司编号
     */
    private Long coNo;
    /**
     * datetime(0)  创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    /**
     * varchar(50)  创建人员
     */
    private String creator;
    /**
     * datetime(0)  修改时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date updated;
    /**
     * varchar(50)  修改人员
     */
    private String updater;
    /**
     * timestamp(0)  时间戳
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date ts;
//
//    private String sequenceName;
//    /**
//     * 序列中文名称
//     */
//    private String sequenceCnName;
//
//    private Long sequenceNo;
//    /**
//     * 起始薪资
//     */
//    private BigDecimal salaryOnSet;
//
//    /**
//     * 层差
//     */
//    private BigDecimal salaryInterval;
//
//    /**
//     * 层数
//     */
//    private Integer salaryFloors;

}

package com.wflow.bean.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 人员表
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrmStaffInfo implements Serializable {

    /**
     * bigint(20) UNSIGNED  员工ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long autoNo;
    /**
     * varchar(50)  员工姓名
     */
    private String staffName;
    /**
     * varchar(20)  员工工号
     */
    private String staffNumber;
    /**
     * varchar(18)  身份证号
     */
    private String identityCard;
    /**
     * varchar(20)  籍贯
     */
    private String nativePlace;
    /**
     * tinyint(2)  性别(0:男;1:女;)
     */
    private Integer gender;
    /**
     * date  出生年月
     */
    @JSONField(format = "yyyy-MM-dd")
    private Date birthday;
    /**
     * varchar(10)  民族
     */
    private String nation;
    /**
     * varchar(50)  个人照片
     */
    private String personalPhoto;
    /**
     * tinyint(2)  婚姻状态
     */
    private Integer marriageStatus;
    /**
     * varchar(10)  政治面貌
     */
    private String politicCountenance;
    /**
     * varchar(20)  身高体重
     */
    private String heightWeight;
    /**
     * varchar(10)  血型
     */
    private String bloodType;
    /**
     * bigint(20)  所属部门ID
     */
    private Long deptNo;
    /**
     * varchar(50)  所属部门
     */
    private String deptName;
    /**
     * bigint(20)  所属岗位ID
     */
    private Long positionNo;
    /**
     * varchar(50)  所属岗位
     */
    private String positionName;
    /**
     * varchar(50)  直属领导
     */
    private String directLeader;
    /**
     * bigint(20)  人事专员ID
     */
    private Long hrbpCode;
    /**
     * varchar(50)  人事专员
     */
    private String hrbpName;
    /**
     * decimal(11, 2)  试用期薪资
     */
    private BigDecimal probationSalary;

    /**
     * 试用期时长
     */
    private Integer probationTime;

    /**
     * decimal(11, 2)  转正薪资
     */
    private BigDecimal regularSalary;
    /**
     * tinyint(2)  试用期延期(0:试用期未延期;1:试用期延期)
     */
    private Integer probationDelayFlag;
    /**
     * varchar(50)  毕业学校
     */
    private String highSchool;
    /**
     * varchar(10)  第一学历
     */
    private Integer firstEducation;
    /**
     * date  入职日期
     */
    @JSONField(format = "yyyy-MM-dd")
    private Date boardDate;
    /**
     * varchar(5)  社会工龄
     */
    private String socialSeniority;
    /**
     * tinyint(2)  在职状态(0:在职试用;1:在职;2:离职)
     */
    private Integer staffStatus;
    /**
     * bigint(20)  部门带教ID
     */
    private Long deptTeachingCode;
    /**
     * varchar(50)  部门带教
     */
    private String deptTeachingName;
    /**
     * tinyint(2)  用工属性(0:正式;1:非正式;2:其他)
     */
    private Integer employAttribute;
    /**
     * varchar(100)  附件
     */
    private String attachment;

    /**
     * 定薪定级状态 0 未定薪定级 1定级中 2已完成
     */
    private Integer salaryGradingStatus;
    /**
     * bigint(20)  系统用户id
     */
    private Long userId;
    /**
     * varchar(50)  用户名
     */
    private String userName;
    /**
     * varchar(50)  姓名
     */
    private String nickName;
    /**
     * varchar(100)  描述
     */
    private String userDesc;

    /**
     * 是否试用期 0否 1是  默认为0
     */
    private Integer isSetProbation;

    /**
     * 是否开通账户 0:否 1：是 默认为0
     */
    private Integer isOpenAccount;

    /**
     * 系统角色编号
     */
    private Long sysRoleNo;
    /**
     * 系统角色编号
     */
    private String qywechatId;
    /**
     * 系统角色编号
     */
    private String dingdingId;
    /**
     * 系统角色编号
     */
    private String feishuId;
    /**
     * datetime  创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    /**
     * varchar(50)  创建人员
     */
    private String creator;
    /**
     * tinyint(2) UNSIGNED  启用状态(0:不启用; 1:启用)
     */
    private Integer enabled;
    /**
     * bigint(11)  公司编号
     */
    private Long coNo;
    /**
     * datetime  修改时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date updated;
    /**
     * varchar(50)  修改人员
     */
    private String updater;
    /**
     * tinyint(2)  是否已删除：0-未删除，1-已删除
     */
    private Integer isDeleted = 0;
    /**
     * timestamp  时间戳
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date ts;

    /**
     * varchar(11)  手机号
     */
    private String phoneNumber;
    /**
     * varchar(50)  邀请公司
     */
    private String inviteCompany;
    /**
     * varchar(50)  档案链接
     */
    private String archivesLink;

    /**
     * int(5)  所属职等
     */
    private Integer positionLevel;
}

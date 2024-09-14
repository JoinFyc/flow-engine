package com.wflow.bean.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 部门表
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgDept implements Serializable {

    /**
     * bigint(20) UNSIGNED  部门ID
     */
    private Long autoNo;
    /**
     * varchar(50)  部门名称
     */
    private String name;
    /**
     * varchar(50)  部门编码
     */
    private String code;
    /**
     * int(11)  排序
     */
    private Integer sortOrder;
    /**
     * bigint(20)  所属部门
     */
    private Long parentDeptId;
    /**
     * varchar(50)  所属部门名称
     */
    private String parentDeptName;
    /**
     * bigint(20)  组织属性ID
     */
    private Long orgId;
    /**
     * varchar(50)  组织属性名称
     */
    private String orgName;
    /**
     * tinyint(4) UNSIGNED  部门类型(1-职能部门 2-业务部门 3-生产部门 4-其他)
     */
    private Integer deptType;
    /**
     * bigint(20)  部门负责人
     */
    private Long responsibleId;
    /**
     * varchar(50)  部门负责人姓名
     */
    private String responsibleName;
    /**
     * bigint(20)  成本中心ID
     */
    private Long costId;
    /**
     * varchar(50)  成本中心名称
     */
    private String costName;
    /**
     * bigint(20)  法律主体ID
     */
    private Long enterpriseSubjectId;
    /**
     * varchar(50)  法律主体名称
     */
    private String enterpriseSubjectName;
    /**
     * bigint(20)  职场表ID
     */
    private Long workplaceId;
    /**
     * varchar(50)  办公地点
     */
    private String workplaceName;
    /**
     * tinyint(4) UNSIGNED  是否有排班(0-没有 1-有)
     */
    private Integer isSchedual;
    /**
     * int(11)  部门编制
     */
    private Integer deptAuthorized;
    /**
     * tinyint(4) UNSIGNED  是否人事部门标记(0-未标记 1-已标记)
     */
    private Integer isHr;
    /**
     * 企业微信ID
     */
    private Long qywechatId;
    /**
     * 企业微信父部门ID
     */
    private Long qywechatParentId;
    /**
     * 钉钉ID
     */
    private Long dingdingId;
    /**
     * 钉钉父部门ID
     */
    private Long dingdingParentId;
    /**
     * 飞书ID
     */
    private Long feishuId;
    /**
     * 飞书父部门ID
     */
    private Long feishuParentId;

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
     * datetime  创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    /**
     * varchar(50)  创建人员
     */
    private String creator;
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
     * timestamp  时间戳
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date ts;

}
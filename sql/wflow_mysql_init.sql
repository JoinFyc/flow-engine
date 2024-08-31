/*
 Navicat Premium Data Transfer

 Source Server         : 本机
 Source Server Type    : MySQL
 Source Server Version : 50728
 Source Host           : localhost:3306
 Source Schema         : wflow_pro

 Target Server Type    : MySQL
 Target Server Version : 50728
 File Encoding         : 65001

 Date: 22/09/2022 14:13:53
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ----------------------------
-- Table structure for wflow_cc_tasks
-- ----------------------------
CREATE TABLE IF NOT EXISTS `wflow_cc_tasks`  (
    `id` bigint NOT NULL,
    `instance_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '审批实例ID',
    `user_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '抄送用户',
    `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板编号',
    `node_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '抄送节点ID',
    `node_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '抄送节点名称',
    `create_time` datetime(3) NOT NULL COMMENT '抄送时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `instance_id` (`instance_id`,`user_id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
SET FOREIGN_KEY_CHECKS = 1;


    -- ----------------------------
-- Table structure for wflow_departments
-- ----------------------------
CREATE TABLE IF NOT EXISTS `wflow_departments`  (
    `id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门id',
    `dept_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名',
    `leader` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门主管',
    `parent_id` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父部门id',
    `created` datetime DEFAULT NULL COMMENT '创建时间',
    `updated` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `dept_id` (`id`) USING BTREE,
    KEY `parent_id` (`parent_id`) USING BTREE,
    KEY `leader` (`leader`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';
-- ----------------------------
-- Records of wflow_departments
-- ----------------------------
INSERT INTO `wflow_departments` VALUES ('35453', '业务部', '3286432', '4319868', '2020-09-16 13:30:37', '2022-09-25 17:49:29');
INSERT INTO `wflow_departments` VALUES ('231535', '生产管理部', NULL, '1486186', '2020-09-16 13:30:39', '2020-09-16 13:30:42');
INSERT INTO `wflow_departments` VALUES ('264868', '行政人事部', NULL, '1486186', '2020-09-16 13:30:42', '2020-09-16 13:30:44');
INSERT INTO `wflow_departments` VALUES ('689698', '客服部', '489564', '4319868', '2020-09-16 13:30:34', '2022-09-04 18:26:20');
INSERT INTO `wflow_departments` VALUES ('1486186', 'xx科技有限公司', '381496', '0', '2020-09-16 13:26:25', '2022-09-04 18:25:12');
INSERT INTO `wflow_departments` VALUES ('4319868', '销售服务部', '927438', '1486186', '2020-09-16 13:30:44', '2022-09-04 18:26:07');
INSERT INTO `wflow_departments` VALUES ('6179678', '研发部', '6418616', '1486186', '2020-09-16 13:26:56', '2022-09-04 18:25:49');


SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_model_groups`  (
    `group_id` bigint NOT NULL,
    `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分组名',
    `sort` int NOT NULL COMMENT '排序',
    `updated` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`group_id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
SET FOREIGN_KEY_CHECKS = 1;
-- ----------------------------
-- Records of wflow_model_groups
-- ----------------------------
INSERT INTO `wflow_model_groups` VALUES (104, 'pro后端流程引擎测试-请勿动', 1, '2022-08-25 17:01:03');
INSERT INTO `wflow_model_groups` VALUES (0, '已停用', 9999, '2022-08-25 17:01:03');
INSERT INTO `wflow_model_groups` VALUES (1, '其他', 9998, '2022-08-25 17:01:03');

UPDATE `wflow_model_groups` SET group_id = 0 WHERE group_name = '已停用';
UPDATE `wflow_model_groups` SET group_id = 1 WHERE group_name = '其他';


CREATE TABLE IF NOT EXISTS `wflow_model_historys`  (
    `id` bigint NOT NULL,
    `process_def_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程定义的ID',
    `deploy_id` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程部署ID',
    `form_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `form_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `business_event_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `version` int NOT NULL DEFAULT '0',
    `logo` json NOT NULL,
    `settings` json NOT NULL,
    `group_id` bigint NOT NULL,
    `form_items` json NOT NULL,
    `form_abstracts` json DEFAULT NULL COMMENT '表单摘要字段信息',
    `form_config` json DEFAULT NULL COMMENT '表单全局设置',
    `process` json NOT NULL,
    `process_config` json DEFAULT NULL COMMENT '流程附加设置项',
    `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `created` datetime DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `form_id` (`form_id`,`version`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='历史表单流程模型表，每次保存/发布新增一条记录';
SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_model_perms`  (
    `id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `form_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表单流程ID',
    `perm_type` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限类型 user = 人、dept = 部门',
    `org_id` varchar(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门ID或人员ID',
    `create_time` datetime DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `form_id` (`form_id`,`perm_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='表单流程发起权限表';
SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_models`  (
    `form_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表单ID',
    `process_def_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程定义ID',
    `deploy_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部署后的ID',
    `version` int NOT NULL DEFAULT '1' COMMENT '当前版本',
    `form_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表单名称',
    `business_event_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `logo` json NOT NULL COMMENT '图标配置',
    `settings` json NOT NULL COMMENT '设置项',
    `group_id` bigint NOT NULL COMMENT '分组ID',
    `form_items` json NOT NULL COMMENT '表单设置内容',
    `form_config` json DEFAULT NULL COMMENT '表单全局设置',
    `form_abstracts` json DEFAULT NULL COMMENT '表单摘要字段集合',
    `process` json NOT NULL COMMENT '流程设置内容',
    `process_config` json DEFAULT NULL COMMENT '流程附加设置项',
    `remark` varchar(125) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    `sort` int NOT NULL,
    `is_delete` bit(1) NOT NULL DEFAULT b'0',
    `is_stop` bit(1) NOT NULL DEFAULT b'0' COMMENT '0 正常 1=停用 2=已删除',
    `created` datetime NOT NULL COMMENT '创建时间',
    `updated` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`form_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_notifys`  (
    `id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
    `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户id',
    `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息类型',
    `instance_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批实例ID',
    `node_id` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对应流程节点ID',
    `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容',
    `readed` bit(1) DEFAULT b'0' COMMENT '是否已读',
    `link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '跳转链接',
    `create_time` datetime DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_id` (`user_id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户消息通知表';
SET FOREIGN_KEY_CHECKS = 1;



CREATE TABLE IF NOT EXISTS `wflow_roles`  (
    `role_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色ID',
    `created` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wflow_roles
-- ----------------------------
INSERT INTO `wflow_roles` VALUES ('BOOS', '董事长', '2022-09-04 18:22:18');
INSERT INTO `wflow_roles` VALUES ('HR', '人事', '2022-09-04 18:22:47');
INSERT INTO `wflow_roles` VALUES ('WFLOW_APPROVAL_ADMIN', '审批管理员', '2022-09-04 18:14:16');

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS `wflow_user_agents`  (
    `user_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
    `agent_user_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '审批代理人ID',
    `start_time` datetime NOT NULL COMMENT '代理开始日期',
    `end_time` datetime DEFAULT NULL COMMENT '代理结束日期',
    `create_time` datetime DEFAULT NULL,
    PRIMARY KEY (`user_id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='审批代理人设置表';
SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_user_departments`  (
    `id` bigint NOT NULL,
    `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
    `dept_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门ID',
    `created` datetime DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `user_id` (`user_id`,`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户部门关系表';
-- ----------------------------
-- Records of wflow_user_departments
-- ----------------------------
INSERT INTO `wflow_user_departments` VALUES (1, '381496', '1486186', '2022-07-05 17:42:17');
INSERT INTO `wflow_user_departments` VALUES (2, '489564', '689698', '2022-07-05 17:42:34');
INSERT INTO `wflow_user_departments` VALUES (3, '568898', '4319868', '2022-07-05 17:42:52');
INSERT INTO `wflow_user_departments` VALUES (4, '6418616', '6179678', '2022-07-05 17:43:09');
INSERT INTO `wflow_user_departments` VALUES (5, '61769798', '231535', '2022-07-05 17:43:24');
INSERT INTO `wflow_user_departments` VALUES (6, '327382', '6179678', '2022-09-04 18:17:55');
INSERT INTO `wflow_user_departments` VALUES (7, '8902743', '689698', '2022-09-04 18:19:12');
INSERT INTO `wflow_user_departments` VALUES (8, '927438', '4319868', '2022-09-04 18:21:08');
INSERT INTO `wflow_user_departments` VALUES (9, '3286432', '35453', '2022-09-25 17:49:19');
INSERT INTO `wflow_user_departments` VALUES (10, '3243678', '35453', '2022-09-25 17:50:58');
INSERT INTO `wflow_user_departments` VALUES (11, '489564', '264868', '2022-09-25 17:50:58');

SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_user_roles`  (
    `id` bigint NOT NULL,
    `user_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户ID',
    `role_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签ID',
    `created` datetime DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `user_id` (`user_id`,`role_id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户标签关系表';
INSERT INTO `wflow_user_roles` VALUES (1, '381496', 'BOOS', '2022-09-25 17:50:58');
INSERT INTO `wflow_user_roles` VALUES (2, '489564', 'HR', '2022-09-25 17:50:58');
INSERT INTO `wflow_user_roles` VALUES (3, '6418616', 'WFLOW_APPROVAL_ADMIN', '2022-09-25 17:50:58');

SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_users`  (
    `user_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户id',
    `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
    `pingyin` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拼音  全拼',
    `py` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拼音, 首字母缩写',
    `alisa` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
    `avatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像base64',
    `sign` text COLLATE utf8mb4_general_ci COMMENT '默认签字',
    `sex` bit(1) DEFAULT b'1' COMMENT '性别',
    `entry_date` date DEFAULT NULL COMMENT '入职日期',
    `leave_date` date DEFAULT NULL COMMENT '离职日期',
    `created` datetime DEFAULT NULL COMMENT '创建时间',
    `updated` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`) USING BTREE,
    KEY `user_id` (`user_id`) USING BTREE,
    KEY `leave_date` (`leave_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';
-- ----------------------------
-- Records of wflow_users
-- ----------------------------
INSERT INTO `wflow_users` VALUES ('327382', '李富贵', 'lifugui', 'lfg', NULL, 'https://dd-static.jd.com/ddimg/jfs/t1/188230/26/28979/10654/633026fdEf64e5e84/fc5c07ab3d5eac19.png', NULL, b'1', '2022-09-23', NULL, '2022-09-04 18:14:53', '2022-09-25 18:01:41');
INSERT INTO `wflow_users` VALUES ('381496', '旅人', 'lvren', 'lr', 'lr', 'https://pic.rmb.bdstatic.com/bjh/203726324a891b1946ba223209cb3fee.png', NULL, b'1', '2020-09-16', NULL, '2020-09-16 13:33:41', '2022-08-14 11:27:49');
INSERT INTO `wflow_users` VALUES ('489564', '李秋香', 'liqiuxiang', 'lqx', 'lqx', NULL, NULL, b'1', '2020-09-16', NULL, '2020-09-16 13:35:40', '2022-06-27 16:48:25');
INSERT INTO `wflow_users` VALUES ('568898', '王翠花', 'wangcuihua', 'wch', 'wch', 'https://dd-static.jd.com/ddimg/jfs/t1/204270/25/26917/8646/63302601E2794a142/5b75f81e6d0c4856.png', NULL, b'1', '2020-09-16', NULL, '2020-09-16 13:35:01', '2022-09-25 17:57:29');
INSERT INTO `wflow_users` VALUES ('927438', '隔壁老王', 'gebilaowang', 'gblw', NULL, 'https://dd-static.jd.com/ddimg/jfs/t1/21515/30/18678/11719/633025abEe734404d/c2950fef75e96028.png', NULL, b'1', NULL, NULL, '2022-09-04 18:16:51', '2022-09-25 17:56:04');
INSERT INTO `wflow_users` VALUES ('3243678', '狗剩', 'gousheng', 'gs', NULL, 'https://dd-static.jd.com/ddimg/jfs/t1/177987/31/29200/17909/63302676E5c00167f/13c59e53269e9f67.png', NULL, b'1', NULL, NULL, '2022-09-25 17:50:15', '2022-09-25 17:59:25');
INSERT INTO `wflow_users` VALUES ('3286432', '铁蛋', 'tiedan', 'td', NULL, 'https://dd-static.jd.com/ddimg/jfs/t1/203154/8/26845/14302/633026b7Ea9b381f7/7e7c5d96fcda0d39.png', NULL, b'1', NULL, NULL, '2022-09-25 17:48:39', '2022-09-25 18:00:28');
INSERT INTO `wflow_users` VALUES ('6418616', '张三', 'zhangsan', 'zs', 'zs', NULL, NULL, b'1', '2020-09-16', NULL, '2020-09-16 13:32:25', '2022-06-27 16:48:32');
INSERT INTO `wflow_users` VALUES ('8902743', '张秋梅', 'zhengqiumei', 'zqm', NULL, NULL, NULL, b'1', NULL, NULL, '2022-09-04 18:16:12', '2022-09-04 18:16:13');
INSERT INTO `wflow_users` VALUES ('61769798', '李四', 'lisi', 'ls', 'ls', NULL, NULL, b'1', '2019-09-16', NULL, '2020-09-16 13:33:00', '2022-06-27 16:48:34');

SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE IF NOT EXISTS `wflow_sub_groups` (
    `group_id` bigint NOT NULL COMMENT '分组id',
    `group_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '分组名',
    `sort` int DEFAULT NULL COMMENT '排序号',
    `created` datetime DEFAULT NULL COMMENT '创建时间',
    `updated` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`group_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='子流程分组表';
INSERT INTO `wflow_sub_groups`(`group_id`, `group_name`, `sort`, `created`, `updated`) VALUES (0, '已停用', 99999, '2023-11-24 15:18:53', '2023-11-24 15:18:56');
UPDATE `wflow_sub_groups` SET `group_id` = 0 WHERE `group_name` = '已停用';


CREATE TABLE IF NOT EXISTS `wflow_sub_process` (
    `id` varchar(50) COLLATE utf8mb4_bin NOT NULL COMMENT '子流程id',
    `proc_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '流程编号',
    `proc_def_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '子流程定义id',
    `deploy_id` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL,
    `process` json DEFAULT NULL COMMENT '子流程设计json',
    `proc_name` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '子流程名称',
    `version` tinyint DEFAULT NULL COMMENT '子流程版本号',
    `group_id` bigint DEFAULT NULL COMMENT '分组id',
    `sort` tinyint DEFAULT NULL COMMENT '排序',
    `is_stop` bit(1) DEFAULT b'0' COMMENT '是否已停用',
    `is_deleted` bit(1) DEFAULT NULL COMMENT '已删除',
    `remark` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
    `created` datetime DEFAULT NULL COMMENT '创建时间',
    `updated` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `proc_code` (`proc_code`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='子流程设计表';

CREATE TABLE IF NOT EXISTS `wflow_form_data` (
    `id` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '主键',
    `instance_id` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '流程实例id',
    `version` int DEFAULT NULL COMMENT '该流程版本',
    `code` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '流程编号',
    `def_id` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '流程定义ID',
    `field_id` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '表单字段ID',
    `field_key` varchar(40) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '表单字段key',
    `field_name` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '表单字段名称',
    `field_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '表单字段组件类型',
    `is_json` bit(1) NOT NULL DEFAULT b'0' COMMENT '字段值是否为json',
    `field_value` text COLLATE utf8mb4_bin NOT NULL COMMENT '表单字段值',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `instance_id` (`instance_id`,`field_id`),
    KEY `field_key` (`field_key`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='流程表单数据表';

CREATE TABLE IF NOT EXISTS `wflow_form_record` (
    `id` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '主键',
    `instance_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '流程实例ID',
    `field_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '字段id',
    `old_value` text COLLATE utf8mb4_bin COMMENT '旧的值',
    `new_value` text COLLATE utf8mb4_bin COMMENT '新的值',
    `create_time` datetime NOT NULL COMMENT '修改的时间',
    `update_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '修改人ID',
    PRIMARY KEY (`id`),
    KEY `instance_id` (`instance_id`,`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='表单字段版本修改记录表';

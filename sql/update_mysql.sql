/* 2023-07-13 更新*/

/*新增字段formConfig，存储表单联动逻辑*/
ALTER TABLE wflow_models ADD form_config json;
ALTER TABLE wflow_model_historys ADD form_config json;

/*修改时间字段为精确到毫秒，修复抄送顺序显示问题*/
ALTER TABLE wflow_cc_tasks MODIFY COLUMN create_time datetime(3);


/*2023-10-22 更新，新增用户表内字段sign用于保存用户签名*/
ALTER TABLE wflow_users ADD sign text;


/*2023-11-7 更新，新增流程模型表里面的流程监听器设置项*/
ALTER TABLE wflow_models ADD process_config json;
ALTER TABLE wflow_model_historys ADD process_config json;

/*2023-12-12 更新，设置流程model的部署id及消息表节点ID*/
ALTER TABLE wflow_model_historys ADD deploy_id VARCHAR(40);
ALTER TABLE wflow_notifys ADD node_id VARCHAR(40);
-- 然后又新增2张表 wflow_sub_groups 、 wflow_sub_process
CREATE TABLE IF NOT EXISTS `wflow_sub_groups` (
    `group_id` int NOT NULL AUTO_INCREMENT COMMENT '分组id',
    `group_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '分组名',
    `sort` int DEFAULT NULL COMMENT '排序号',
    `created` datetime DEFAULT NULL COMMENT '创建时间',
    `updated` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`group_id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='子流程分组表';

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
    `group_id` int DEFAULT NULL COMMENT '分组id',
    `sort` tinyint DEFAULT NULL COMMENT '排序',
    `is_stop` bit(1) DEFAULT b'0' COMMENT '是否已停用',
    `is_deleted` bit(1) DEFAULT NULL COMMENT '已删除',
    `remark` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
    `created` datetime DEFAULT NULL COMMENT '创建时间',
    `updated` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `proc_code` (`proc_code`,`version`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='子流程设计表';

-- 2024-2-7 添加摘要字段，表单摘要字段单独拿出来存储 {字段ID: '字段名称'}
ALTER TABLE wflow_models ADD form_abstracts json DEFAULT NULL COMMENT '表单摘要字段';
ALTER TABLE wflow_model_historys ADD form_abstracts json DEFAULT NULL COMMENT '表单摘要字段';

-- 2024-2-26 添加两张表，流程表单数据表和流程表单修改记录表
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

/** 2024-05-10 更新，将所有自增字段去除自增属性，使用bigint存储**/
ALTER TABLE wflow_cc_tasks MODIFY COLUMN id bigint;
ALTER TABLE wflow_model_groups MODIFY COLUMN group_id bigint;
ALTER TABLE wflow_model_historys MODIFY COLUMN id bigint;
ALTER TABLE wflow_model_historys MODIFY COLUMN group_id bigint;
ALTER TABLE wflow_models MODIFY COLUMN group_id bigint;
ALTER TABLE wflow_sub_groups MODIFY COLUMN id bigint;
ALTER TABLE wflow_sub_process MODIFY COLUMN group_id bigint;
ALTER TABLE wflow_model_perms MODIFY COLUMN org_id varchar(40);
ALTER TABLE wflow_user_roles MODIFY COLUMN id bigint;
ALTER TABLE wflow_departments MODIFY COLUMN id varchar(20);
ALTER TABLE wflow_user_departments MODIFY COLUMN id bigint;


-- ============================================================
-- flow 工作流数据库 · 全部建表脚本（共 17 张表）
-- 本文件已同步本地数据库 flow（同步时间：2026-08-09），
-- 表结构/字段注释/索引/自增值均以数据库实际为准。
-- 设计原则：
--   1. 模板元数据与业务填报数据分离，动态表单不新增数据表，采用 JSON 存储字段配置
--   2. 流程节点独立成表（flow_template_node），任务流转按节点记录（flow_task_node）
--   3. 主键统一 bigint，存储引擎 InnoDB，字符集 utf8mb4
-- 执行：mysql -uroot -p12345 < flow_schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `flow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `flow`;

-- ============================================================
-- 表1：sys_user 系统用户表
-- ============================================================
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '账号',
  `emp_no` varchar(50) NOT NULL COMMENT '员工号',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `password` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0禁用 1正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_emp_no` (`emp_no`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户';

-- ============================================================
-- 表2：flow_template 流程模板表（核心主表）
-- ============================================================
CREATE TABLE `flow_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) NOT NULL COMMENT '流程模板名称',
  `category` varchar(50) DEFAULT NULL COMMENT '模板分类',
  `version` int NOT NULL DEFAULT '1' COMMENT '模板版本号',
  `version_desc` varchar(500) DEFAULT NULL COMMENT '当前版本改动说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0停用 1启用',
  `creator_id` bigint NOT NULL COMMENT '创建管理员id',
  `modifier_id` bigint DEFAULT NULL COMMENT '最近修改人ID',
  `dept_id` bigint DEFAULT NULL COMMENT '创建人部门ID（部门内可见，样例公共可见）',
  `is_sample` tinyint NOT NULL DEFAULT '0' COMMENT '1=样例(公共可见不可改) 0=普通',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dept_sample` (`dept_id`,`is_sample`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板';

-- ============================================================
-- 表3：flow_template_node 流程节点表【核心】
-- 存储模板的有序节点链定义（node_type: 1开始 2中间 3结束）
-- ============================================================
CREATE TABLE `flow_template_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称',
  `sort_num` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `node_type` tinyint NOT NULL DEFAULT '2' COMMENT '节点类型 1开始 2中间 3结束',
  `node_tips` varchar(500) DEFAULT NULL COMMENT '节点处理提示文案',
  `guide_text` text COMMENT '节点填写说明（处理人查看）',
  `guide_files` json DEFAULT NULL COMMENT '节点说明文件 [{"name":"","url":""}]',
  `next_handler_tip` varchar(500) DEFAULT NULL COMMENT '下一步处理人提示（提交节点时展示给处理人）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_template_sort` (`template_id`,`sort_num`)
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板节点（节点链）';

-- ============================================================
-- 表4：flow_template_field 模板表单字段表
-- field_role 标识填写方式（1创建人 2处理人）；bind_node_id 为处理人填写字段绑定节点
-- ============================================================
CREATE TABLE `flow_template_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `node_id` bigint DEFAULT NULL COMMENT '归属节点ID flow_template_node.id',
  `field_role` tinyint NOT NULL DEFAULT '1' COMMENT '1=创建人填写 2=处理人填写',
  `field_key` varchar(64) NOT NULL COMMENT '字段唯一标识（英文编码，form_field_001）',
  `field_label` varchar(100) NOT NULL COMMENT '字段中文名称',
  `field_type` varchar(30) NOT NULL COMMENT '字段类型：text单行、textarea多行、number数字、date日期、radio单选、checkbox多选、file文件、image图片',
  `sort_num` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `required` tinyint NOT NULL DEFAULT '0' COMMENT '是否必填 0否1是',
  `placeholder` varchar(200) DEFAULT NULL COMMENT '输入框提示文字',
  `field_tips` varchar(500) DEFAULT NULL COMMENT '字段下方提示说明',
  `max_length` int DEFAULT NULL COMMENT '文本最大长度',
  `enum_options` json DEFAULT NULL COMMENT '枚举选项 [{"label":"选项1","value":"1"},{"label":"选项2","value":"2"}]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `bind_node_id` bigint DEFAULT NULL COMMENT '处理人填写字段的绑定节点',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB AUTO_INCREMENT=832 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板自定义表单字段';

-- ============================================================
-- 表5：flow_template_version 模板版本记录表
-- ============================================================
CREATE TABLE `flow_template_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `version` int NOT NULL COMMENT '版本号',
  `version_desc` varchar(500) DEFAULT NULL COMMENT '版本改动说明',
  `modifier_id` bigint DEFAULT NULL COMMENT '修改人ID',
  `modifier_name` varchar(50) DEFAULT NULL COMMENT '修改人姓名',
  `config_snapshot` json DEFAULT NULL COMMENT '模板配置快照（节点+字段JSON）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板版本记录';

-- ============================================================
-- 表6：flow_task 填报任务表
-- 一次下发 = 一个任务组，下发给每个人一条独立任务（成员任务）
-- ============================================================
CREATE TABLE `flow_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `dispatch_id` bigint DEFAULT NULL COMMENT '下发批次ID（同一次下发/跟进的独立任务共享，= 组内首条任务ID）',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `template_version` int NOT NULL COMMENT '锁定模板版本，防止模板修改影响已有任务',
  `task_name` varchar(100) NOT NULL COMMENT '本次任务名称',
  `task_desc` text COMMENT '任务说明',
  `start_time` datetime DEFAULT NULL COMMENT '填报开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '填报截止时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '任务状态 1进行中 2已结束 3作废',
  `current_node_id` bigint DEFAULT NULL COMMENT '当前节点ID（流转指针）',
  `current_handler_id` bigint DEFAULT NULL COMMENT '当前处理人ID（流转指针）',
  `finished_node_count` int NOT NULL DEFAULT '0' COMMENT '已完成节点数',
  `total_node_count` int NOT NULL DEFAULT '0' COMMENT '总节点数',
  `creator_id` bigint NOT NULL COMMENT '下发任务管理员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `template_data` text COMMENT 'template fields json',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_current_handler` (`current_handler_id`),
  KEY `idx_dispatch` (`dispatch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='填报任务（管理员下发任务）';

-- ============================================================
-- 表7：flow_task_dispatch 期次表
-- 任务按周期自动/手动生成的每期，为每位配置人员创建提交任务
-- ============================================================
CREATE TABLE `flow_task_dispatch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主任务ID（下发批次）',
  `task_id` bigint DEFAULT NULL COMMENT '所属任务ID flow_dispatch.id',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `template_version` int NOT NULL COMMENT '锁定模板版本',
  `period_no` int DEFAULT NULL COMMENT '期次序号',
  `period_name` varchar(100) DEFAULT NULL COMMENT '期次名称（如 2026-08 第1期）',
  `period_key` varchar(50) DEFAULT NULL COMMENT '期间标识（自动下发防重复检测，如 2026-Q3）',
  `manual_flag` tinyint NOT NULL DEFAULT '0' COMMENT '期次来源 0自动下发 1手动临时期次',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_desc` text,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `template_data` text COMMENT 'template fields json',
  `template_fields_json` text COMMENT '模板级字段定义JSON（快照，node_id为空的创建人/处理人字段）',
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='期次（任务按周期自动或手动生成的每期，为每位配置人员创建提交任务）';

-- ============================================================
-- 表8：flow_task_dispatch_node 期次节点快照表
-- 下发期次时锁定当期模板节点全部配置（提示/填写说明/说明文件/下一步提示/字段定义），
-- 防模板后续升级影响历史期次；临时人员新增时也从该快照复制节点
-- ============================================================
CREATE TABLE `flow_task_dispatch_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dispatch_id` bigint NOT NULL COMMENT '期次ID flow_task_dispatch.id',
  `node_id` bigint DEFAULT NULL COMMENT '来源模板节点ID flow_template_node.id（溯源）',
  `node_name` varchar(100) DEFAULT NULL COMMENT '节点名称',
  `sort_num` int DEFAULT NULL COMMENT '节点顺序',
  `node_type` int DEFAULT NULL COMMENT '节点类型 1开始 2中间 3结束',
  `node_tips` varchar(1000) DEFAULT NULL COMMENT '节点提示（快照）',
  `guide_text` varchar(2000) DEFAULT NULL COMMENT '节点填写说明（快照）',
  `guide_files` text COMMENT '说明文件JSON（快照）',
  `next_handler_tip` varchar(500) DEFAULT NULL COMMENT '下一步处理人提示（快照）',
  `fields_json` text COMMENT '节点字段定义JSON（快照，含绑定该节点的处理人字段）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dispatch` (`dispatch_id`,`sort_num`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='期次节点快照';

-- ============================================================
-- 表9：flow_task_node 任务流转节点表【核心】
-- 任务实际流转记录，每个节点一条；action/reject_reason/pass_comment 存处理动作与意见
-- dispatch_node_id 引用期次节点快照（无则回退模板解析）
-- ============================================================
CREATE TABLE `flow_task_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `node_id` bigint NOT NULL COMMENT '模板节点ID',
  `dispatch_node_id` bigint DEFAULT NULL COMMENT '期次节点快照ID flow_task_dispatch_node.id（无则回退模板解析）',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称',
  `sort_num` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `node_type` tinyint NOT NULL DEFAULT '2' COMMENT '节点类型 1开始 2中间 3结束',
  `handler_user_id` bigint DEFAULT NULL COMMENT '本节点处理人ID',
  `submit_status` tinyint NOT NULL DEFAULT '0' COMMENT '处理状态 0待处理 1已处理',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `next_handler_user_id` bigint DEFAULT NULL COMMENT '指定的下一节点处理人ID',
  `form_record_id` bigint DEFAULT NULL COMMENT '本节点表单记录ID',
  `base_data` varchar(2000) DEFAULT NULL COMMENT '处理人填写的任务基础字段值(JSON fieldId->value)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `action` tinyint DEFAULT '0' COMMENT '操作类型 0通过 1退回',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '退回原因（action=1 时填写）',
  `pass_comment` varchar(500) DEFAULT NULL COMMENT '通过意见（action=0时填写，非必填）',
  PRIMARY KEY (`id`),
  KEY `idx_task_sort` (`task_id`,`sort_num`),
  KEY `idx_handler_status` (`handler_user_id`,`submit_status`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务流转节点记录';

-- ============================================================
-- 表10：flow_task_member 任务人员配置表
-- 生成期次时抄用，每位人员创建独立提交任务
-- ============================================================
CREATE TABLE `flow_task_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务ID flow_dispatch.id',
  `user_id` bigint NOT NULL COMMENT '人员用户ID sys_user.id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_user` (`task_id`,`user_id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务人员配置（生成期次时为每位人员创建提交任务，可在任务中变更）';

-- ============================================================
-- 表11：flow_task_log 任务流转通知/催办日志表
-- ============================================================
CREATE TABLE `flow_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint DEFAULT NULL COMMENT '成员任务ID',
  `dispatch_id` bigint DEFAULT NULL COMMENT '下发批次ID',
  `task_node_id` bigint DEFAULT NULL COMMENT '任务流转节点ID',
  `node_id` bigint DEFAULT NULL COMMENT '模板节点ID',
  `log_type` tinyint NOT NULL DEFAULT '1' COMMENT '1流转通知 2催办',
  `content` varchar(500) DEFAULT NULL COMMENT '日志内容',
  `handler_user_id` bigint DEFAULT NULL COMMENT '目标处理人ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_dispatch` (`dispatch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务流转通知/催办日志';

-- ============================================================
-- 表12：flow_form_record 表单填报主记录
-- 每个节点处理人提交生成一条，含 node_id、task_node_id
-- ============================================================
CREATE TABLE `flow_form_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '填报记录主键',
  `task_id` bigint NOT NULL COMMENT '所属任务',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `node_id` bigint DEFAULT NULL COMMENT '节点ID',
  `task_node_id` bigint DEFAULT NULL COMMENT '任务流转节点ID flow_task_node.id',
  `user_id` bigint NOT NULL COMMENT '填报人',
  `record_status` tinyint NOT NULL DEFAULT '1' COMMENT '1正常 2作废',
  `is_draft` tinyint NOT NULL DEFAULT '0' COMMENT '0正式提交 1草稿',
  `submit_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_user` (`task_id`,`user_id`),
  KEY `idx_task_node` (`task_node_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单填报主记录';

-- ============================================================
-- 表13：flow_form_data 表单字段填写数据表（核心动态数据存储）
-- ============================================================
CREATE TABLE `flow_form_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '关联填报主记录ID flow_form_record.id',
  `field_id` bigint NOT NULL COMMENT '模板字段ID flow_template_field.id',
  `field_key` varchar(64) NOT NULL COMMENT '字段标识，冗余方便查询',
  `field_value` text COMMENT '填报值；文件类型存附件id逗号分隔/JSON数组',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单填报明细数据';

-- ============================================================
-- 表14：attach 附件表（文件/图片上传，biz_id 关联业务，ecs_url 随机字符）
-- 字段按业务要求固定，不许减少或变更
-- ============================================================
CREATE TABLE `attach` (
  `attach_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'id',
  `biz_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '业务id',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '文件名',
  `ecs_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'ecs地址',
  `creator` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`attach_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='附件表';

-- ============================================================
-- 表15：flow_dispatch 任务表（一次下发计划 = 一个任务）
-- 周期/截止/催办等下发配置独立存储于 flow_dispatch_config（每任务一份）
-- ============================================================
CREATE TABLE `flow_dispatch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_data` text COMMENT '模板级字段值 JSON（任务级配置，生成期次时抄用）',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_desc` text COMMENT '任务说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0停用 1启用',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `dept_id` bigint DEFAULT NULL COMMENT '创建人部门ID（部门内可见，样例公共可见）',
  `is_sample` tinyint NOT NULL DEFAULT '0' COMMENT '1=样例(公共可见不可改) 0=普通',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`),
  KEY `idx_dept_sample` (`dept_id`,`is_sample`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务（含模板配置信息与下发周期配置，生成期次时抄用配置与人员）';

-- ============================================================
-- 表16：flow_dispatch_config 任务下发配置表（每任务一份）
-- 周期类型/触发日/截止天数/催办天数独立建表，配置一次长期复用
-- ============================================================
CREATE TABLE `flow_dispatch_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务ID（每任务一份配置）',
  `cycle_type` int DEFAULT NULL COMMENT '周期类型 1每周 2每月 3每季度 4单次下发',
  `cycle_day` int DEFAULT NULL COMMENT '触发日：周(1-7周一=1)/月(1-31)/季(1-31)',
  `deadline_days` int DEFAULT NULL COMMENT '截止：触发日后N天截止（单次按下发日起算）',
  `urge_days` int DEFAULT NULL COMMENT '截止前N天自动催办',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务下发配置（每任务一份，独立于任务表存储）';

-- ============================================================
-- 表17：flow_dispatch_config_template 下发配置模板库
-- 新建任务时可一键拉取复用，避免反复配置出错
-- ============================================================
CREATE TABLE `flow_dispatch_config_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `cycle_type` tinyint NOT NULL COMMENT '周期类型 1每周 2每月 3每季度 4单次下发',
  `cycle_day` int DEFAULT NULL COMMENT '触发日：周(1-7)/月(1-31)',
  `deadline_days` int NOT NULL COMMENT '截止天数：触发日后N天',
  `urge_days` int DEFAULT '0' COMMENT '催办天数',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `dept_id` bigint DEFAULT NULL COMMENT '创建人部门ID（部门内可见，样例公共可见）',
  `is_sample` tinyint NOT NULL DEFAULT '0' COMMENT '1=样例(公共可见不可改) 0=普通',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dept_sample` (`dept_id`,`is_sample`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='下发配置模板（新建任务时可拉取复用）';

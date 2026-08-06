-- ============================================================
-- flow 工作流数据库 · 全部建表脚本（共 17 张表）
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
  `real_name` varchar(50) NOT NULL COMMENT '员工姓名',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `password` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0禁用 1正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_username (`username`),
  UNIQUE KEY uk_emp_no (`emp_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ============================================================
-- 表2：flow_template 流程模板表（核心主表）
-- ============================================================
CREATE TABLE `flow_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) NOT NULL COMMENT '流程模板名称',
  `category` varchar(50) DEFAULT NULL COMMENT '模板分类',
  `version` int NOT NULL DEFAULT 1 COMMENT '模板版本号',
  `version_desc` varchar(500) DEFAULT NULL COMMENT '当前版本改动说明',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `creator_id` bigint NOT NULL COMMENT '创建管理员id',
  `modifier_id` bigint DEFAULT NULL COMMENT '最近修改人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板';

-- ============================================================
-- 表3：flow_template_node 流程节点表【核心】
-- 存储模板的有序节点链定义（node_type: 1开始 2中间 3结束）
-- ============================================================
CREATE TABLE `flow_template_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `node_type` tinyint NOT NULL DEFAULT 2 COMMENT '节点类型 1开始 2中间 3结束',
  `node_tips` varchar(500) DEFAULT NULL COMMENT '节点处理提示文案',
  `guide_text` text COMMENT '节点填写说明（处理人查看）',
  `guide_files` json DEFAULT NULL COMMENT '节点说明文件 [{"name":"","url":""}]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_template_sort (`template_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板节点（节点链）';

-- ============================================================
-- 表4：flow_template_field 模板表单字段表
-- field_role 标识填写方式（1创建人 2处理人）；bind_node_id 为处理人填写字段绑定节点
-- ============================================================
CREATE TABLE `flow_template_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `node_id` bigint DEFAULT NULL COMMENT '归属节点ID flow_template_node.id',
  `field_role` tinyint NOT NULL DEFAULT 1 COMMENT '填写方式 1创建人填写 2处理人填写（任务基础字段专用）',
  `field_key` varchar(64) NOT NULL COMMENT '字段唯一标识（英文编码）',
  `field_label` varchar(100) NOT NULL COMMENT '字段中文名称',
  `field_type` varchar(30) NOT NULL COMMENT '字段类型：text/textarea/number/date/radio/checkbox/file/image',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `required` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填 0否1是',
  `placeholder` varchar(200) DEFAULT NULL COMMENT '输入框提示文字',
  `field_tips` varchar(500) DEFAULT NULL COMMENT '字段下方提示说明',
  `max_length` int DEFAULT NULL COMMENT '文本最大长度',
  `enum_options` json DEFAULT NULL COMMENT '枚举选项 [{"label":"选项1","value":"1"}]',
  `bind_node_id` bigint DEFAULT NULL COMMENT '绑定流程节点ID（field_role=2 时生效）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_template_id (`template_id`),
  KEY idx_node_id (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板自定义表单字段';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本记录';

-- ============================================================
-- 表6：flow_task 填报任务表
-- 一次下发 = 一个任务组，下发给每个人一条独立任务（成员任务）
-- ============================================================
CREATE TABLE `flow_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `dispatch_id` bigint DEFAULT NULL COMMENT '下发批次ID（= 期次ID flow_task_dispatch.id）',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `template_version` int NOT NULL COMMENT '锁定模板版本',
  `task_name` varchar(100) NOT NULL COMMENT '本次任务名称',
  `task_desc` text DEFAULT NULL COMMENT '任务说明',
  `start_time` datetime DEFAULT NULL COMMENT '填报开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '填报截止时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '任务状态 1进行中 2已结束(全流程完成) 3作废',
  `current_node_id` bigint DEFAULT NULL COMMENT '当前节点ID（流转指针）',
  `current_handler_id` bigint DEFAULT NULL COMMENT '当前处理人ID（流转指针）',
  `finished_node_count` int NOT NULL DEFAULT 0 COMMENT '已完成节点数',
  `total_node_count` int NOT NULL DEFAULT 0 COMMENT '总节点数',
  `creator_id` bigint NOT NULL COMMENT '下发任务管理员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `template_data` text DEFAULT NULL COMMENT '模板级字段值 JSON {fieldId:value}',
  PRIMARY KEY (`id`),
  KEY idx_dispatch (`dispatch_id`),
  KEY idx_template_id (`template_id`),
  KEY idx_current_handler (`current_handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报任务（期次下发给每位人员一条独立任务）';

-- ============================================================
-- 表7：flow_task_dispatch 期次表
-- 任务按周期自动/手动生成的每期，为每位配置人员创建提交任务
-- ============================================================
CREATE TABLE `flow_task_dispatch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主任务ID（下发批次）',
  `task_id` bigint DEFAULT NULL COMMENT '所属任务ID flow_dispatch.id',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `template_version` int NOT NULL COMMENT '锁定模板版本',
  `dispatch_plan_id` bigint DEFAULT NULL COMMENT '下发计划ID flow_dispatch.id（旧字段，停用）',
  `period_no` int DEFAULT NULL COMMENT '期次序号',
  `period_name` varchar(100) DEFAULT NULL COMMENT '期次名称（如 2026-Q3）',
  `period_key` varchar(50) DEFAULT NULL COMMENT '期间标识（自动下发防重复检测，如 2026-Q3）',
  `manual_flag` tinyint NOT NULL DEFAULT 0 COMMENT '期次来源 0自动下发 1手动临时期次',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_desc` text DEFAULT NULL COMMENT '任务说明',
  `start_time` datetime DEFAULT NULL COMMENT '填报开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '填报截止时间',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `template_data` text DEFAULT NULL COMMENT '模板级字段值 JSON',
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期次（任务按周期自动或手动生成，为每位配置人员创建提交任务）';

-- ============================================================
-- 表8：flow_task_user 任务填报人员关联表【废弃，保留兼容】
-- 顺序流转模型改用 flow_task_node 承载处理人，此表停止使用
-- ============================================================
CREATE TABLE `flow_task_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '填报人用户ID',
  `submit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未填报 1草稿 2已提交 3作废',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_task_user (`task_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务-填报人员关联（废弃兼容）';

-- ============================================================
-- 表9：flow_task_node 任务流转节点表【核心】
-- 任务实际流转记录，每个节点一条；action/reject_reason/pass_comment 存处理动作与意见
-- ============================================================
CREATE TABLE `flow_task_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `node_id` bigint NOT NULL COMMENT '模板节点ID flow_template_node.id',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称（冗余）',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `node_type` tinyint NOT NULL DEFAULT 2 COMMENT '节点类型 1开始 2中间 3结束',
  `handler_user_id` bigint DEFAULT NULL COMMENT '本节点处理人ID',
  `submit_status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态 0待处理 1已处理',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `next_handler_user_id` bigint DEFAULT NULL COMMENT '指定的下一节点处理人ID',
  `form_record_id` bigint DEFAULT NULL COMMENT '本节点提交的表单记录ID flow_form_record.id',
  `base_data` varchar(2000) DEFAULT NULL COMMENT '本节点提交时处理人填写的任务基础字段值 JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `action` tinyint DEFAULT 0 COMMENT '处理动作 0通过 1退回',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '退回原因',
  `pass_comment` varchar(500) DEFAULT NULL COMMENT '通过意见',
  PRIMARY KEY (`id`),
  KEY idx_task_sort (`task_id`, `sort_num`),
  KEY idx_handler_status (`handler_user_id`, `submit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转节点记录';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务人员配置（生成期次时为每位人员创建提交任务）';

-- ============================================================
-- 表11：flow_task_log 任务流转通知/催办日志表
-- ============================================================
CREATE TABLE `flow_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint DEFAULT NULL COMMENT '成员任务ID',
  `dispatch_id` bigint DEFAULT NULL COMMENT '下发批次ID',
  `task_node_id` bigint DEFAULT NULL COMMENT '任务流转节点ID flow_task_node.id',
  `node_id` bigint DEFAULT NULL COMMENT '模板节点ID flow_template_node.id',
  `log_type` tinyint NOT NULL DEFAULT 1 COMMENT '日志类型 1流转通知 2催办',
  `content` varchar(500) DEFAULT NULL COMMENT '日志内容',
  `handler_user_id` bigint DEFAULT NULL COMMENT '目标处理人ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_dispatch` (`dispatch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转通知/催办日志';

-- ============================================================
-- 表12：flow_form_record 表单填报主记录
-- 每个节点处理人提交生成一条，含 node_id、task_node_id
-- ============================================================
CREATE TABLE `flow_form_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '填报记录主键',
  `task_id` bigint NOT NULL COMMENT '所属任务',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `node_id` bigint DEFAULT NULL COMMENT '节点ID flow_template_node.id',
  `task_node_id` bigint DEFAULT NULL COMMENT '任务流转节点ID flow_task_node.id',
  `user_id` bigint NOT NULL COMMENT '填报人',
  `record_status` tinyint NOT NULL DEFAULT 1 COMMENT '1正常 2作废',
  `is_draft` tinyint NOT NULL DEFAULT 0 COMMENT '0正式提交 1草稿',
  `submit_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_task_user (`task_id`,`user_id`),
  KEY idx_task_node (`task_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单填报主记录';

-- ============================================================
-- 表13：flow_form_data 表单字段填写数据表（核心动态数据存储）
-- ============================================================
CREATE TABLE `flow_form_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '关联填报主记录ID flow_form_record.id',
  `field_id` bigint NOT NULL COMMENT '模板字段ID flow_template_field.id',
  `field_key` varchar(64) NOT NULL COMMENT '字段标识，冗余方便查询',
  `field_value` text DEFAULT NULL COMMENT '填报值；文件类型存文件名/JSON数组',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_record_id (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单填报明细数据';

-- ============================================================
-- 表14：flow_attachment 附件文件表
-- ============================================================
CREATE TABLE `flow_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint DEFAULT NULL COMMENT '关联填报记录ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT 'oss/minio存储路径',
  `file_size` bigint NOT NULL COMMENT '文件大小byte',
  `file_suffix` varchar(20) DEFAULT NULL COMMENT '后缀',
  `upload_user_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_record_id (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件文件';

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
  `period_type` varchar(10) DEFAULT 'month' COMMENT '周期类型 week/month/quarter（废弃保留）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务（含模板配置信息，生成期次时抄用配置与人员）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务下发配置（每任务一份，独立于任务表存储）';

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
  `urge_days` int DEFAULT 0 COMMENT '催办天数',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下发配置模板（新建任务时可拉取复用）';

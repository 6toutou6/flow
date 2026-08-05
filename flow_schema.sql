-- ============================================================
-- flow 数据库建表脚本（线性顺序流转工作流版）
-- 设计原则：
--   1. 模板元数据与业务填报数据分离，动态表单不新增数据表，采用 JSON 存储字段配置
--   2. 流程节点独立成表（flow_template_node），任务流转按节点记录（flow_task_node）
--   3. 主键统一 bigint，存储引擎 InnoDB，字符集 utf8mb4
-- 说明：本脚本与实际数据库结构保持一致（含 field_role、bind_node_id、base_data、
--       action、reject_reason、pass_comment 等新增列）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `flow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `flow`;

-- ============================================================
-- 一、完整建表（全新建库直接执行本段）
-- ============================================================

-- 表1：sys_user 用户表（系统基础用户）
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

-- 表2：flow_template 流程模板表（核心主表）
-- 顺序流转模型：去掉 flow_mode、enable_repeat_submit、global_tips、enable_modify
CREATE TABLE `flow_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) NOT NULL COMMENT '流程模板名称',
  `category` varchar(50) DEFAULT NULL COMMENT '模板分类',
  `version` int NOT NULL DEFAULT 1 COMMENT '模板版本号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `creator_id` bigint NOT NULL COMMENT '创建管理员id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板';

-- 表3：flow_template_node 流程节点表【核心】
-- 存储模板的有序节点链定义（node_key、assign_next 已去除）
CREATE TABLE `flow_template_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称（如：需求设计）',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号，从小到大',
  `node_type` tinyint NOT NULL DEFAULT 2 COMMENT '节点类型 1开始 2中间 3结束',
  `node_tips` varchar(500) DEFAULT NULL COMMENT '节点处理提示文案',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_template_sort (`template_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板节点（节点链）';

-- 表4：flow_template_field 模板表单字段表
-- 每个节点的字段配置：node_id 标识字段归属节点；
-- field_role 标识填写方式（1创建人 2处理人，任务基础字段专用，node_id 为空）；
-- bind_node_id 为处理人填写字段绑定流程节点（仅该节点处理时填写）
CREATE TABLE `flow_template_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `node_id` bigint DEFAULT NULL COMMENT '归属节点ID flow_template_node.id',
  `field_role` tinyint NOT NULL DEFAULT 1 COMMENT '填写方式 1创建人填写 2处理人填写（任务基础字段专用）',
  `field_key` varchar(64) NOT NULL COMMENT '字段唯一标识（英文编码，自动生成）',
  `field_label` varchar(100) NOT NULL COMMENT '字段中文名称',
  `field_type` varchar(30) NOT NULL COMMENT '字段类型：text单行、textarea多行、number数字、date日期、radio单选、checkbox多选、file文件、image图片',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `required` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填 0否1是',
  `placeholder` varchar(200) DEFAULT NULL COMMENT '输入框提示文字',
  `field_tips` varchar(500) DEFAULT NULL COMMENT '字段下方提示说明',
  `max_length` int DEFAULT NULL COMMENT '文本最大长度',
  `enum_options` json DEFAULT NULL COMMENT '枚举选项 [{"label":"选项1","value":"1"}]',
  `bind_node_id` bigint DEFAULT NULL COMMENT '绑定流程节点ID flow_template_node.id（field_role=2 时生效，仅该节点处理时填写）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_template_id (`template_id`),
  KEY idx_node_id (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板自定义表单字段';

-- 表5：flow_task 填报任务表（一次下发 = 一个任务组；下发给每个人一条独立任务）
-- 流转指针字段 + 下发批次ID（任务→人员分组键）
CREATE TABLE `flow_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `dispatch_id` bigint DEFAULT NULL COMMENT '下发批次ID（同一次下发/跟进的独立任务共享，= 组内首条任务ID）',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `template_version` int NOT NULL COMMENT '锁定模板版本，防止模板修改影响已有任务',
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
  `template_data` text DEFAULT NULL COMMENT '模板级字段值 JSON {fieldId:value}（下发时创建人赋值）',
  PRIMARY KEY (`id`),
  KEY idx_dispatch (`dispatch_id`),
  KEY idx_template_id (`template_id`),
  KEY idx_current_handler (`current_handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报任务（一次下发一个任务组，下发给每个人一条独立任务）';

-- 表5.1：flow_task_dispatch 主任务表（一次下发 = 一条主任务）
-- 人员成员删空后主任务仍保留，可在数据后台单独删除（须无成员）
CREATE TABLE `flow_task_dispatch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主任务ID（下发批次）',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `template_version` int NOT NULL COMMENT '锁定模板版本',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_desc` text DEFAULT NULL COMMENT '任务说明',
  `start_time` datetime DEFAULT NULL COMMENT '填报开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '填报截止时间',
  `creator_id` bigint NOT NULL COMMENT '下发任务管理员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `template_data` text DEFAULT NULL COMMENT '模板级字段值 JSON {fieldId:value}（下发时创建人赋值，新增成员任务时复制）',
  PRIMARY KEY (`id`),
  KEY idx_template (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主任务（一次下发，人员删空后仍保留，可删除）';

-- 表6：flow_task_user 任务填报人员关联表【废弃，保留兼容】
-- 顺序流转模型改用 flow_task_node 承载处理人，此表停止使用，保留不删
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

-- 表7：flow_task_node 任务流转节点表【核心】
-- 任务实际流转记录，每个节点一条；
-- base_data 存处理人填写的任务基础字段值；action/reject_reason/pass_comment 存处理动作与意见
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
  `base_data` varchar(2000) DEFAULT NULL COMMENT '本节点提交时处理人填写的任务基础字段值 JSON {fieldId:value}',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `action` tinyint DEFAULT 0 COMMENT '处理动作 0通过 1退回',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '退回原因',
  `pass_comment` varchar(500) DEFAULT NULL COMMENT '通过意见',
  PRIMARY KEY (`id`),
  KEY idx_task_sort (`task_id`, `sort_num`),
  KEY idx_handler_status (`handler_user_id`, `submit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转节点记录';

-- 表8：flow_form_record 表单填报主记录
-- 每个节点处理人提交生成一条，含 node_id、task_node_id
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

-- 表9：flow_form_data 表单字段填写数据表（核心动态数据存储）
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

-- 表10：flow_attachment 附件文件表
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
-- 二、升级脚本（已有旧库时执行本段，从并行收集模型升级到顺序流转模型）
-- 注意：执行前先备份；本段会清空 flow_* 测试数据
-- ============================================================

-- 1. 清空旧测试数据（sys_user 保留）
TRUNCATE TABLE `flow_form_data`;
TRUNCATE TABLE `flow_attachment`;
TRUNCATE TABLE `flow_form_record`;
TRUNCATE TABLE `flow_task_user`;
TRUNCATE TABLE `flow_task`;
TRUNCATE TABLE `flow_task_dispatch`;
TRUNCATE TABLE `flow_task_node`;
TRUNCATE TABLE `flow_template_field`;
TRUNCATE TABLE `flow_template_node`;
TRUNCATE TABLE `flow_template`;

-- 2. 清理 flow_template：去掉已废弃列（flow_mode、enable_repeat_submit、global_tips、enable_modify）
ALTER TABLE `flow_template` DROP COLUMN `flow_mode`;
ALTER TABLE `flow_template` DROP COLUMN `enable_repeat_submit`;
ALTER TABLE `flow_template` DROP COLUMN `global_tips`;
ALTER TABLE `flow_template` DROP COLUMN `enable_modify`;

-- 3. 清理/改造 flow_template_node：去掉 node_key、assign_next（按位置自动判定）
ALTER TABLE `flow_template_node` DROP COLUMN `node_key`;
ALTER TABLE `flow_template_node` DROP COLUMN `assign_next`;

-- 4. 改造 flow_template_field：加 node_id、field_role、bind_node_id；去掉已废弃文件列
ALTER TABLE `flow_template_field` ADD COLUMN `node_id` bigint DEFAULT NULL COMMENT '归属节点ID flow_template_node.id' AFTER `template_id`;
ALTER TABLE `flow_template_field` ADD COLUMN `field_role` tinyint NOT NULL DEFAULT 1 COMMENT '填写方式 1创建人填写 2处理人填写（任务基础字段专用）' AFTER `node_id`;
ALTER TABLE `flow_template_field` ADD COLUMN `bind_node_id` bigint DEFAULT NULL COMMENT '绑定流程节点ID（field_role=2 时生效）' AFTER `enum_options`;
ALTER TABLE `flow_template_field` ADD INDEX `idx_node_id` (`node_id`);
ALTER TABLE `flow_template_field` DROP COLUMN `file_max_size`;
ALTER TABLE `flow_template_field` DROP COLUMN `file_suffix`;
ALTER TABLE `flow_template_field` DROP COLUMN `multiple_file`;

-- 5. 改造 flow_task：加流转指针字段 + template_data 位置调整（放末尾）
ALTER TABLE `flow_task`
  ADD COLUMN `current_node_id` bigint DEFAULT NULL COMMENT '当前节点ID（流转指针）' AFTER `status`,
  ADD COLUMN `current_handler_id` bigint DEFAULT NULL COMMENT '当前处理人ID（流转指针）' AFTER `current_node_id`,
  ADD COLUMN `finished_node_count` int NOT NULL DEFAULT 0 COMMENT '已完成节点数' AFTER `current_handler_id`,
  ADD COLUMN `total_node_count` int NOT NULL DEFAULT 0 COMMENT '总节点数' AFTER `finished_node_count`,
  ADD INDEX `idx_current_handler` (`current_handler_id`);

-- 6. 改造 flow_form_record：加 node_id、task_node_id
ALTER TABLE `flow_form_record`
  ADD COLUMN `node_id` bigint DEFAULT NULL COMMENT '节点ID' AFTER `template_id`,
  ADD COLUMN `task_node_id` bigint DEFAULT NULL COMMENT '任务流转节点ID flow_task_node.id' AFTER `node_id`,
  ADD INDEX `idx_task_node` (`task_node_id`);

-- 7. 新增 flow_template_node（旧库无此表时创建）
CREATE TABLE IF NOT EXISTS `flow_template_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `node_type` tinyint NOT NULL DEFAULT 2 COMMENT '节点类型 1开始 2中间 3结束',
  `node_tips` varchar(500) DEFAULT NULL COMMENT '节点处理提示文案',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_template_sort (`template_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板节点（节点链）';

-- 8. 新增 flow_task_node（旧库无此表时创建；含 base_data、action、reject_reason、pass_comment）
CREATE TABLE IF NOT EXISTS `flow_task_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `node_id` bigint NOT NULL COMMENT '模板节点ID',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `node_type` tinyint NOT NULL DEFAULT 2 COMMENT '节点类型 1开始 2中间 3结束',
  `handler_user_id` bigint DEFAULT NULL COMMENT '本节点处理人ID',
  `submit_status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态 0待处理 1已处理',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `next_handler_user_id` bigint DEFAULT NULL COMMENT '指定的下一节点处理人ID',
  `form_record_id` bigint DEFAULT NULL COMMENT '本节点表单记录ID',
  `base_data` varchar(2000) DEFAULT NULL COMMENT '处理人填写的任务基础字段值 JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `action` tinyint DEFAULT 0 COMMENT '处理动作 0通过 1退回',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '退回原因',
  `pass_comment` varchar(500) DEFAULT NULL COMMENT '通过意见',
  PRIMARY KEY (`id`),
  KEY idx_task_sort (`task_id`, `sort_num`),
  KEY idx_handler_status (`handler_user_id`, `submit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转节点记录';

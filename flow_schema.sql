-- ============================================================
-- flow 数据库建表脚本
-- 设计原则：模板元数据与业务填报数据分离，动态表单不新增数据表，采用JSON存储字段配置
-- 优势：新增字段类型无需改表，真正通用化
-- 主键统一 bigint，存储引擎 InnoDB，字符集 utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `flow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `flow`;

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
CREATE TABLE `flow_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) NOT NULL COMMENT '流程模板名称',
  `category` varchar(50) DEFAULT NULL COMMENT '模板分类',
  `global_tips` text DEFAULT NULL COMMENT '流程全局填报提示（富文本）',
  `flow_mode` tinyint NOT NULL DEFAULT 1 COMMENT '流程模式：1单人 2多人并行 3顺序流转',
  `version` int NOT NULL DEFAULT 1 COMMENT '模板版本号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `enable_repeat_submit` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许重复填报',
  `enable_modify` tinyint NOT NULL DEFAULT 0 COMMENT '提交后是否允许修改',
  `creator_id` bigint NOT NULL COMMENT '创建管理员id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板';

-- 表3：flow_template_field 模板表单字段表【最重要】
-- 存储管理员拖拽配置的每一个表单字段
CREATE TABLE `flow_template_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `field_key` varchar(64) NOT NULL COMMENT '字段唯一标识（英文编码，form_field_001）',
  `field_label` varchar(100) NOT NULL COMMENT '字段中文名称',
  `field_type` varchar(30) NOT NULL COMMENT '字段类型：text单行、textarea多行、number数字、date日期、radio单选、checkbox多选、file文件、image图片',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `required` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填 0否1是',
  `placeholder` varchar(200) DEFAULT NULL COMMENT '输入框提示文字',
  `field_tips` varchar(500) DEFAULT NULL COMMENT '字段下方提示说明',
  `max_length` int DEFAULT NULL COMMENT '文本最大长度',
  -- 枚举配置：单选/多选的选项JSON
  `enum_options` json DEFAULT NULL COMMENT '枚举选项 [{"label":"选项1","value":"1"},{"label":"选项2","value":"2"}]',
  -- 文件上传配置
  `file_max_size` bigint DEFAULT NULL COMMENT '文件最大大小 KB',
  `file_suffix` varchar(200) DEFAULT NULL COMMENT '允许后缀 .pdf,.jpg,.png',
  `multiple_file` tinyint NOT NULL DEFAULT 0 COMMENT '是否支持多文件上传',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_template_id (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板自定义表单字段';

-- 表4：flow_task 填报任务表（管理员下发一次任务生成一条任务）
CREATE TABLE `flow_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `template_id` bigint NOT NULL COMMENT '关联流程模板ID',
  `template_version` int NOT NULL COMMENT '锁定模板版本，防止模板修改影响已有任务',
  `task_name` varchar(100) NOT NULL COMMENT '本次任务名称',
  `task_desc` text DEFAULT NULL COMMENT '任务说明',
  `start_time` datetime DEFAULT NULL COMMENT '填报开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '填报截止时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '任务状态 1进行中 2已结束 3作废',
  `creator_id` bigint NOT NULL COMMENT '下发任务管理员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_template_id (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='填报任务（管理员下发任务）';

-- 表5：flow_task_user 任务填报人员关联表
-- 任务下发时，批量插入需要填报的人员
CREATE TABLE `flow_task_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '填报人用户ID',
  `submit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未填报 1草稿 2已提交 3作废',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_task_user (`task_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务-填报人员关联';

-- 表6：flow_form_record 表单填报主记录
-- 每个人每次提交生成一条主记录
CREATE TABLE `flow_form_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '填报记录主键',
  `task_id` bigint NOT NULL COMMENT '所属任务',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `user_id` bigint NOT NULL COMMENT '填报人',
  `record_status` tinyint NOT NULL DEFAULT 1 COMMENT '1正常 2作废',
  `is_draft` tinyint NOT NULL DEFAULT 0 COMMENT '0正式提交 1草稿',
  `submit_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_task_user (`task_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单填报主记录';

-- 表7：flow_form_data 表单字段填写数据表（核心动态数据存储）
-- 存储用户每一个字段填写的值
CREATE TABLE `flow_form_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '关联填报主记录ID flow_form_record.id',
  `field_id` bigint NOT NULL COMMENT '模板字段ID flow_template_field.id',
  `field_key` varchar(64) NOT NULL COMMENT '字段标识，冗余方便查询',
  -- 文本、日期、数字、单选多选全部存在value；文件存储文件ID数组
  `field_value` text DEFAULT NULL COMMENT '填报值；文件类型存附件id逗号分隔/JSON数组',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_record_id (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单填报明细数据';

-- 表8：flow_attachment 附件文件表
-- 统一管理所有上传文件，解耦表单
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

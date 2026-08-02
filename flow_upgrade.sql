-- 线性顺序流转工作流升级脚本（从并行收集模型升级）
USE `flow`;

-- 1. 清空旧测试数据（sys_user 保留）
TRUNCATE TABLE `flow_form_data`;
TRUNCATE TABLE `flow_attachment`;
TRUNCATE TABLE `flow_form_record`;
TRUNCATE TABLE `flow_task_user`;
TRUNCATE TABLE `flow_task`;
TRUNCATE TABLE `flow_template_field`;
TRUNCATE TABLE `flow_template`;

-- 2. 改造 flow_template：去掉 flow_mode、enable_repeat_submit
ALTER TABLE `flow_template` DROP COLUMN `flow_mode`;
ALTER TABLE `flow_template` DROP COLUMN `enable_repeat_submit`;

-- 3. 改造 flow_template_field：加 node_id
ALTER TABLE `flow_template_field` ADD COLUMN `node_id` bigint DEFAULT NULL COMMENT '归属节点ID flow_template_node.id' AFTER `template_id`;
ALTER TABLE `flow_template_field` ADD INDEX `idx_node_id` (`node_id`);

-- 4. 改造 flow_task：加流转指针字段
ALTER TABLE `flow_task`
  ADD COLUMN `current_node_id` bigint DEFAULT NULL COMMENT '当前节点ID（流转指针）' AFTER `status`,
  ADD COLUMN `current_handler_id` bigint DEFAULT NULL COMMENT '当前处理人ID（流转指针）' AFTER `current_node_id`,
  ADD COLUMN `finished_node_count` int NOT NULL DEFAULT 0 COMMENT '已完成节点数' AFTER `current_handler_id`,
  ADD COLUMN `total_node_count` int NOT NULL DEFAULT 0 COMMENT '总节点数' AFTER `finished_node_count`,
  ADD INDEX `idx_current_handler` (`current_handler_id`);

-- 5. 改造 flow_form_record：加 node_id、task_node_id
ALTER TABLE `flow_form_record`
  ADD COLUMN `node_id` bigint DEFAULT NULL COMMENT '节点ID' AFTER `template_id`,
  ADD COLUMN `task_node_id` bigint DEFAULT NULL COMMENT '任务流转节点ID flow_task_node.id' AFTER `node_id`,
  ADD INDEX `idx_task_node` (`task_node_id`);

-- 6. 新增 flow_template_node
CREATE TABLE `flow_template_node` (
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

-- 7. 新增 flow_task_node
CREATE TABLE `flow_task_node` (
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
  `action` tinyint DEFAULT 0 COMMENT '操作类型 0通过 1退回',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '退回原因（action=1 时填写）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_task_sort (`task_id`, `sort_num`),
  KEY idx_handler_status (`handler_user_id`, `submit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务流转节点记录';

-- 验证
SHOW TABLES;
SELECT 'flow_template columns' AS info;
SHOW COLUMNS FROM `flow_template`;
SELECT 'flow_task columns' AS info;
SHOW COLUMNS FROM `flow_task`;

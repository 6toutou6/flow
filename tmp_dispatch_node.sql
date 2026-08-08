-- 期次节点快照表：下发时锁定当期模板节点配置，防模板升级影响历史期次
CREATE TABLE IF NOT EXISTS flow_task_dispatch_node (
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
  KEY `idx_dispatch` (`dispatch_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期次节点快照';

-- flow_task_node 增加快照引用
ALTER TABLE flow_task_node ADD COLUMN dispatch_node_id bigint DEFAULT NULL COMMENT '期次节点快照ID flow_task_dispatch_node.id（无则回退模板解析）' AFTER node_id;

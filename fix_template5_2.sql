-- 补充执行：整改审批(27)/下发人审核(28)/行领导审核(29) 新增字段 + 任务基础字段
USE `flow`;

INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(188, 5, 27, 1, 'approve_result', '审批结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"不通过","value":"2"}]', '选择审批结果'),
(189, 5, 27, 1, 'approve_opinion', '审批意见', 'textarea', 9, 1, NULL, '填写审批意见'),
(190, 5, 27, 1, 'approver', '审批人', 'text', 10, 0, NULL, '审批人姓名'),
(191, 5, 28, 1, 'review_result', '审核结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]', '选择审核结果'),
(192, 5, 28, 1, 'review_opinion', '审核意见', 'textarea', 9, 0, NULL, '填写审核意见'),
(193, 5, 28, 1, 'reviewer', '审核人', 'text', 10, 0, NULL, '审核人姓名'),
(194, 5, 29, 1, 'final_result', '终审结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]', '选择终审结果'),
(195, 5, 29, 1, 'final_opinion', '终审意见', 'textarea', 9, 1, NULL, '填写终审意见'),
(196, 5, 29, 1, 'final_remark', '终审批注', 'textarea', 10, 0, NULL, '补充批注说明');

INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `bind_node_id`, `placeholder`) VALUES
(197, 5, NULL, 1, 'rectify_task_name', '整改任务名称', 'text', 7, 1, NULL, '如：2026年服务问题整改'),
(198, 5, NULL, 1, 'dispatch_unit', '下发单位', 'text', 8, 0, NULL, '如下发整改通知的部门');

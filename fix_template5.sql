-- ============================================================
-- 整改-复杂模板（template_id=5）字段补齐脚本
-- 原则：只增不减，保留原有字段（含用户新加 171-177），新增业务字段
-- ============================================================

USE `flow`;

-- ---------- 一、完善现有占位枚举选项内容（不增减字段） ----------
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"通过","value":"1"},{"label":"不通过","value":"2"}]' WHERE `id` = 151;
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]' WHERE `id` IN (159, 167);
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"制度流程","value":"1"},{"label":"信息系统","value":"2"},{"label":"人员服务","value":"3"}]' WHERE `id` = 136;
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"制度建设","value":"1"},{"label":"系统优化","value":"2"},{"label":"人员培训","value":"3"}]' WHERE `id` = 144;
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"整改措施","value":"1"},{"label":"整改期限","value":"2"},{"label":"佐证材料","value":"3"}]' WHERE `id` IN (152, 160, 168);

-- ---------- 二、问题自查（节点25）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`, `field_tips`) VALUES
(178, 5, 25, 1, 'issue_type', '问题类型', 'radio', 8, 1, '[{"label":"流程类","value":"1"},{"label":"系统类","value":"2"},{"label":"服务类","value":"3"},{"label":"其他","value":"4"}]', NULL, '选择发现问题的类别'),
(179, 5, 25, 1, 'discover_time', '发现问题时间', 'date', 9, 1, NULL, '选择发现问题的时间', NULL),
(180, 5, 25, 1, 'duty_person', '责任人', 'text', 10, 1, NULL, '填写问题责任人姓名', NULL),
(181, 5, 25, 1, 'duty_dept', '所属部门', 'text', 11, 0, NULL, '填写责任人所在部门', NULL),
(182, 5, 25, 1, 'issue_desc', '问题详细描述', 'textarea', 12, 1, NULL, '详细描述问题的现象与影响', NULL),
(183, 5, 25, 1, 'urgency', '紧急程度', 'radio', 13, 0, '[{"label":"紧急","value":"1"},{"label":"重要","value":"2"},{"label":"一般","value":"3"}]', NULL, '按影响程度选择');

-- ---------- 三、问题整改（节点26）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `placeholder`, `field_tips`) VALUES
(184, 5, 26, 1, 'rectify_measure', '整改措施', 'textarea', 8, 1, '填写具体整改措施', '分条描述如何整改'),
(185, 5, 26, 1, 'rectify_deadline', '整改完成日期', 'date', 9, 1, '选择计划完成日期', NULL),
(186, 5, 26, 1, 'rectify_person', '整改责任人', 'text', 10, 1, '填写整改责任人姓名', NULL),
(187, 5, 26, 1, 'progress_percent', '整改进度', 'number', 11, 0, '填写进度百分比（0-100）', NULL);

-- ---------- 四、整改审批（节点27）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(188, 5, 27, 1, 'approve_result', '审批结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"不通过","value":"2"}]', '选择审批结果'),
(189, 5, 27, 1, 'approve_opinion', '审批意见', 'textarea', 9, 1, NULL, '填写审批意见'),
(190, 5, 27, 1, 'approver', '审批人', 'text', 10, 0, NULL, '审批人姓名');

-- ---------- 五、下发人审核（节点28）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(191, 5, 28, 1, 'review_result', '审核结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]', '选择审核结果'),
(192, 5, 28, 1, 'review_opinion', '审核意见', 'textarea', 9, 0, NULL, '填写审核意见'),
(193, 5, 28, 1, 'reviewer', '审核人', 'text', 10, 0, NULL, '审核人姓名');

-- ---------- 六、行领导审核（节点29）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(194, 5, 29, 1, 'final_result', '终审结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]', '选择终审结果'),
(195, 5, 29, 1, 'final_opinion', '终审意见', 'textarea', 9, 1, NULL, '填写终审意见'),
(196, 5, 29, 1, 'final_remark', '终审批注', 'textarea', 10, 0, NULL, '补充批注说明');

-- ---------- 七、任务基础字段（创建人填写，续用户已有字段 sort 7/8） ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `bind_node_id`, `placeholder`) VALUES
(197, 5, NULL, 1, 'rectify_task_name', '整改任务名称', 'text', 7, 1, NULL, '如：2026年服务问题整改'),
(198, 5, NULL, 1, 'dispatch_unit', '下发单位', 'text', 8, 0, NULL, '如下发整改通知的部门');

-- ============================================================
-- 演示模板种子数据（后续使用指导用）
-- 清空业务数据 + 插入 4 个通俗易懂的示例模板
-- 执行：mysql -uroot -p12345 --default-character-set=utf8mb4 flow < demo_templates.sql
-- ============================================================

-- ---------- 一、清空业务数据（保留 sys_user） ----------
TRUNCATE TABLE `flow_form_data`;
TRUNCATE TABLE `flow_attachment`;
TRUNCATE TABLE `flow_form_record`;
TRUNCATE TABLE `flow_task`;
TRUNCATE TABLE `flow_task_dispatch`;
TRUNCATE TABLE `flow_task_node`;
TRUNCATE TABLE `flow_template_field`;
TRUNCATE TABLE `flow_template_node`;
TRUNCATE TABLE `flow_template`;

-- ---------- 二、流程模板 ----------
INSERT INTO `flow_template` (`id`, `template_name`, `category`, `version`, `status`, `creator_id`) VALUES
(1, '考试登记', '考试', 1, 1, 1),
(2, '请假审批', '人事', 1, 1, 1),
(3, '设备领用', '行政', 1, 1, 1),
(4, '费用报销', '财务', 1, 1, 1);

-- ---------- 三、流程节点（node_type: 1开始 2中间 3结束） ----------
-- 模板1 考试登记
INSERT INTO `flow_template_node` (`id`, `template_id`, `node_name`, `sort_num`, `node_type`, `node_tips`) VALUES
(1, 1, '登记考试信息', 0, 1, '填写本次考试的基本信息'),
(2, 1, '资格审核', 1, 2, '审核考生报名资格是否合规'),
(3, 1, '考场安排', 2, 2, '安排考场并通知考生'),
(4, 1, '考试完成', 3, 3, '考试流程全部完成');
-- 模板2 请假审批
INSERT INTO `flow_template_node` (`id`, `template_id`, `node_name`, `sort_num`, `node_type`, `node_tips`) VALUES
(5, 2, '填写请假申请', 0, 1, '请填写请假相关信息'),
(6, 2, '部门主管审批', 1, 2, '主管审核请假申请'),
(7, 2, '人事审批', 2, 2, '人事复核审批结果'),
(8, 2, '审批完成', 3, 3, '请假流程完成');
-- 模板3 设备领用
INSERT INTO `flow_template_node` (`id`, `template_id`, `node_name`, `sort_num`, `node_type`, `node_tips`) VALUES
(9, 3, '填写领用申请', 0, 1, '填写需要领用的设备信息'),
(10, 3, '部门负责人审批', 1, 2, '部门负责人审批领用申请'),
(11, 3, '仓库发放', 2, 2, '仓库按申请发放设备'),
(12, 3, '领用完成', 3, 3, '设备领用流程完成');
-- 模板4 费用报销
INSERT INTO `flow_template_node` (`id`, `template_id`, `node_name`, `sort_num`, `node_type`, `node_tips`) VALUES
(13, 4, '填写报销申请', 0, 1, '填写报销事由与金额'),
(14, 4, '部门审批', 1, 2, '部门负责人审批报销申请'),
(15, 4, '财务审核', 2, 2, '财务审核并安排打款'),
(16, 4, '报销完成', 3, 3, '报销流程完成');

-- ---------- 四、字段（node_id 归属节点；node_id NULL 为任务基础字段，field_role 1创建人 2处理人，bind_node_id 绑定处理人填写节点） ----------
-- 模板1 考试登记：节点字段
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `placeholder`, `field_tips`) VALUES
(1, 1, 1, 1, 'exam_subject', '考试科目', 'text', 0, 1, '如：大学英语四级', '填写本次考试的科目名称'),
(2, 1, 1, 1, 'exam_time', '考试时间', 'date', 1, 1, NULL, '选择考试开始日期'),
(3, 1, 1, 1, 'exam_place', '考试地点', 'text', 2, 1, '如：教学楼A栋101', '填写考试教室位置'),
(4, 1, 1, 1, 'exam_notes', '考试说明', 'textarea', 3, 0, '补充说明，如携带证件要求', NULL);
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(5, 1, 2, 1, 'audit_result', '审核结果', 'radio', 0, 1, '[{"label":"通过","value":"1"},{"label":"不通过","value":"2"}]', NULL),
(6, 1, 2, 1, 'audit_opinion', '审核意见', 'textarea', 1, 0, NULL, '填写审核意见或说明');
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `placeholder`) VALUES
(7, 1, 3, 1, 'room_no', '考场号', 'text', 0, 1, '如：第01考场'),
(8, 1, 3, 1, 'invigilator', '监考老师', 'text', 1, 0, '填写监考老师姓名'),
(9, 1, 3, 1, 'notice_content', '通知内容', 'textarea', 2, 0, '告知考生考试时间地点');
-- 模板1 任务基础字段（创建人填写 / 处理人在"登记考试信息"节点填写）
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `bind_node_id`, `placeholder`) VALUES
(10, 1, NULL, 1, 'exam_name', '考试名称', 'text', 0, 1, NULL, '如：2026年秋季期末考试'),
(11, 1, NULL, 1, 'organizer', '主办单位', 'text', 1, 0, NULL, '如：教务处'),
(12, 1, NULL, 2, 'candidate_name', '考生姓名', 'text', 2, 1, 1, '填写考生姓名'),
(13, 1, NULL, 2, 'ticket_no', '准考证号', 'text', 3, 1, 1, '填写考生准考证号');

-- 模板2 请假审批：节点字段
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`, `field_tips`) VALUES
(14, 2, 5, 1, 'leave_type', '请假类型', 'radio', 0, 1, '[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"},{"label":"调休","value":"4"}]', NULL, NULL),
(15, 2, 5, 1, 'leave_start', '开始时间', 'date', 1, 1, NULL, '选择请假开始日期', NULL),
(16, 2, 5, 1, 'leave_end', '结束时间', 'date', 2, 1, NULL, '选择请假结束日期', NULL);
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `placeholder`) VALUES
(17, 2, 5, 1, 'leave_reason', '请假事由', 'textarea', 3, 1, '请填写请假的详细原因'),
(18, 2, 6, 1, 'mgr_opinion', '主管意见', 'radio', 0, 1, '请选择审批结果'),
(19, 2, 6, 1, 'mgr_comment', '主管批注', 'textarea', 1, 0, '补充批注说明'),
(20, 2, 7, 1, 'hr_opinion', '人事审批意见', 'radio', 0, 1, '请选择审批结果'),
(21, 2, 7, 1, 'hr_comment', '人事批注', 'textarea', 1, 0, '补充批注说明');
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"同意","value":"1"},{"label":"不同意","value":"2"}]' WHERE `id` IN (18, 20);
-- 模板2 任务基础字段
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `bind_node_id`, `placeholder`) VALUES
(22, 2, NULL, 1, 'applicant', '申请人', 'text', 0, 1, NULL, '填写申请人姓名'),
(23, 2, NULL, 1, 'department', '所属部门', 'text', 1, 0, NULL, '如：技术部');

-- 模板3 设备领用：节点字段
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `placeholder`) VALUES
(24, 3, 9, 1, 'device_name', '设备名称', 'text', 0, 1, '如：笔记本电脑'),
(25, 3, 9, 1, 'device_count', '领用数量', 'number', 1, 1, '填写需要领用的数量'),
(26, 3, 9, 1, 'device_purpose', '使用用途', 'textarea', 2, 1, '说明领用后的用途'),
(27, 3, 10, 1, 'dept_opinion', '审批意见', 'radio', 0, 1, '请选择审批结果'),
(28, 3, 10, 1, 'dept_comment', '审批备注', 'textarea', 1, 0, '补充备注说明'),
(29, 3, 11, 1, 'deliver_status', '发放情况', 'radio', 0, 1, '请选择发放情况'),
(30, 3, 11, 1, 'deliver_remark', '发放备注', 'textarea', 1, 0, '备注缺货或发放详情');
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"同意","value":"1"},{"label":"不同意","value":"2"}]' WHERE `id` IN (27);
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"已发放","value":"1"},{"label":"缺货登记","value":"2"}]' WHERE `id` IN (29);
-- 模板3 任务基础字段
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `bind_node_id`, `placeholder`) VALUES
(31, 3, NULL, 1, 'applicant', '申请人', 'text', 0, 1, NULL, '填写申请人姓名'),
(32, 3, NULL, 1, 'phone', '联系方式', 'text', 1, 0, NULL, '如：13800000000');

-- 模板4 费用报销：节点字段
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `placeholder`) VALUES
(33, 4, 13, 1, 'reimburse_reason', '报销事由', 'textarea', 0, 1, '说明报销的具体用途'),
(34, 4, 13, 1, 'reimburse_amount', '报销金额', 'number', 1, 1, '填写报销金额（元）'),
(35, 4, 13, 1, 'expense_date', '发生日期', 'date', 2, 1, '选择费用发生日期'),
(36, 4, 14, 1, 'dept_opinion', '部门审批意见', 'radio', 0, 1, '请选择审批结果'),
(37, 4, 15, 1, 'finance_opinion', '财务审核意见', 'radio', 0, 1, '请选择审核结果'),
(38, 4, 15, 1, 'finance_comment', '打款说明', 'textarea', 1, 0, '填写打款时间或驳回原因');
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"同意","value":"1"},{"label":"不同意","value":"2"}]' WHERE `id` IN (36);
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"同意","value":"1"},{"label":"驳回","value":"2"}]' WHERE `id` IN (37);
-- 模板4 任务基础字段
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `bind_node_id`, `placeholder`) VALUES
(39, 4, NULL, 1, 'reimburse_person', '报销人', 'text', 0, 1, NULL, '填写报销人姓名'),
(40, 4, NULL, 1, 'reimburse_dept', '所属部门', 'text', 1, 0, NULL, '如：技术部');

-- ============================================================
-- 整改-复杂模板（flow_template id=5）完整设计导入脚本
-- 适用前提：库中已存在模板5（模板行 + 节点链 25-29 + 占位字段 131-170）
-- 本脚本完成：
--   1) 节点提示（node_tips）补全
--   2) 占位字段（131-170）按节点业务语义设计：标签/必填/占位/枚举/提示
--   3) 补齐问题自查(25)/整改(26)/审批(27)/下发人审核(28)/行领导审核(29) 业务字段
--   4) 任务基础字段（整改任务名称、下发单位）
-- 执行：mysql -uroot -p12345 < flow_template5_import.sql
-- ============================================================

USE `flow`;

-- ---------- 一、节点提示定义 ----------
UPDATE `flow_template_node` SET `node_tips` = '自查本部门是否存在待整改问题，如实填报自查情况' WHERE `id` = 25;
UPDATE `flow_template_node` SET `node_tips` = '针对自查发现的问题制定整改措施并落实到位' WHERE `id` = 26;
UPDATE `flow_template_node` SET `node_tips` = '对整改方案与落实情况进行审批' WHERE `id` = 27;
UPDATE `flow_template_node` SET `node_tips` = '下发人对整改结果进行现场核实与审核确认' WHERE `id` = 28;
UPDATE `flow_template_node` SET `node_tips` = '行领导终审确认整改闭环，流程结束' WHERE `id` = 29;

-- ---------- 二、占位字段设计（131-170）：标签/必填/占位/枚举/提示 ----------
UPDATE `flow_template_field` SET
  `field_label` = CASE `id`
    WHEN 131 THEN '问题简述'
    WHEN 132 THEN '自查过程说明'
    WHEN 133 THEN '自查次数'
    WHEN 134 THEN '预计整改日期'
    WHEN 135 THEN '问题等级'
    WHEN 136 THEN '影响范围'
    WHEN 137 THEN '佐证材料上传'
    WHEN 138 THEN '现场照片'
    WHEN 139 THEN '整改目标'
    WHEN 140 THEN '整改方式'
    WHEN 141 THEN '整改投入人数'
    WHEN 142 THEN '整改进展日期'
    WHEN 143 THEN '整改优先级'
    WHEN 144 THEN '整改资源投入'
    WHEN 145 THEN '整改材料上传'
    WHEN 146 THEN '整改对比照片'
    WHEN 147 THEN '审批依据'
    WHEN 148 THEN '整改效果评估'
    WHEN 149 THEN '审批轮次'
    WHEN 150 THEN '审批日期'
    WHEN 151 THEN '审批方式'
    WHEN 152 THEN '审批关注事项'
    WHEN 153 THEN '审批材料上传'
    WHEN 154 THEN '现场核验照片'
    WHEN 155 THEN '核实结论'
    WHEN 156 THEN '核实情况说明'
    WHEN 157 THEN '抽查问题数'
    WHEN 158 THEN '核实日期'
    WHEN 159 THEN '审核结论'
    WHEN 160 THEN '核实关注事项'
    WHEN 161 THEN '核实材料上传'
    WHEN 162 THEN '核实现场照片'
    WHEN 163 THEN '整改闭环确认'
    WHEN 164 THEN '整改评价'
    WHEN 165 THEN '通过整改项数'
    WHEN 166 THEN '终审日期'
    WHEN 167 THEN '终审方式'
    WHEN 168 THEN '终审关注事项'
    WHEN 169 THEN '验收材料上传'
    WHEN 170 THEN '验收照片'
    ELSE `field_label` END,
  `required` = CASE `id`
    WHEN 131 THEN 1 WHEN 132 THEN 1 WHEN 133 THEN 1 WHEN 134 THEN 1
    WHEN 135 THEN 1 WHEN 136 THEN 0 WHEN 137 THEN 0 WHEN 138 THEN 0
    WHEN 139 THEN 1 WHEN 140 THEN 1 WHEN 141 THEN 1 WHEN 142 THEN 0
    WHEN 143 THEN 1 WHEN 144 THEN 0 WHEN 145 THEN 0 WHEN 146 THEN 0
    WHEN 147 THEN 1 WHEN 148 THEN 1 WHEN 149 THEN 0 WHEN 150 THEN 1
    WHEN 151 THEN 0 WHEN 152 THEN 0 WHEN 153 THEN 0 WHEN 154 THEN 0
    WHEN 155 THEN 1 WHEN 156 THEN 1 WHEN 157 THEN 0 WHEN 158 THEN 1
    WHEN 159 THEN 1 WHEN 160 THEN 0 WHEN 161 THEN 0 WHEN 162 THEN 0
    WHEN 163 THEN 1 WHEN 164 THEN 1 WHEN 165 THEN 0 WHEN 166 THEN 1
    WHEN 167 THEN 0 WHEN 168 THEN 0 WHEN 169 THEN 0 WHEN 170 THEN 0
    ELSE `required` END,
  `placeholder` = CASE `id`
    WHEN 131 THEN '用一句话概括自查发现的问题'
    WHEN 132 THEN '描述自查范围、方式与经过'
    WHEN 133 THEN '本周期已开展自查的次数'
    WHEN 134 THEN '选择计划完成整改的日期'
    WHEN 137 THEN '上传自查佐证材料'
    WHEN 138 THEN '上传问题现场照片'
    WHEN 139 THEN '填写整改后要达到的目标'
    WHEN 140 THEN '描述采用何种方式开展整改'
    WHEN 141 THEN '参与整改的人员数量'
    WHEN 142 THEN '记录本次整改进展的时间'
    WHEN 145 THEN '上传整改佐证材料'
    WHEN 146 THEN '上传整改前后对比照片'
    WHEN 147 THEN '填写审批依据的标准或制度'
    WHEN 148 THEN '评估整改是否达到预期效果'
    WHEN 149 THEN '第几次审批'
    WHEN 150 THEN '选择审批日期'
    WHEN 153 THEN '上传审批相关材料'
    WHEN 154 THEN '上传现场核验照片'
    WHEN 155 THEN '简述核实结论'
    WHEN 156 THEN '描述核实整改落实的具体情况'
    WHEN 157 THEN '现场抽查发现的问题数量'
    WHEN 158 THEN '选择核实日期'
    WHEN 161 THEN '上传核实材料'
    WHEN 162 THEN '上传核实现场照片'
    WHEN 163 THEN '确认整改是否形成闭环'
    WHEN 164 THEN '评价整改工作整体质量'
    WHEN 165 THEN '本次通过验收的整改项数量'
    WHEN 166 THEN '选择终审日期'
    WHEN 169 THEN '上传验收材料'
    WHEN 170 THEN '上传验收照片'
    ELSE `placeholder` END,
  `enum_options` = CASE `id`
    WHEN 135 THEN '[{"label":"重大","value":"1"},{"label":"一般","value":"2"},{"label":"轻微","value":"3"}]'
    WHEN 136 THEN '[{"label":"零售业务","value":"1"},{"label":"对公业务","value":"2"},{"label":"中后台","value":"3"},{"label":"风险合规","value":"4"},{"label":"客户服务","value":"5"}]'
    WHEN 143 THEN '[{"label":"高","value":"1"},{"label":"中","value":"2"},{"label":"低","value":"3"}]'
    WHEN 144 THEN '[{"label":"人员","value":"1"},{"label":"经费","value":"2"},{"label":"系统","value":"3"},{"label":"制度","value":"4"}]'
    WHEN 151 THEN '[{"label":"线上审批","value":"1"},{"label":"线下审批","value":"2"}]'
    WHEN 167 THEN '[{"label":"线上评审","value":"1"},{"label":"线下评审","value":"2"}]'
    ELSE `enum_options` END,
  `field_tips` = CASE `id`
    WHEN 135 THEN '按问题影响程度选择等级'
    WHEN 136 THEN '可多选，选择问题影响的业务范围'
    WHEN 143 THEN '按紧急程度设置整改优先级'
    WHEN 144 THEN '可多选，说明整改投入的资源'
    WHEN 151 THEN '选择本次审批采取的方式'
    WHEN 167 THEN '选择本次终审采取的方式'
    ELSE `field_tips` END
WHERE `id` BETWEEN 131 AND 170;

-- ---------- 三、完善现有占位枚举选项内容（不增减字段） ----------
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"通过","value":"1"},{"label":"不通过","value":"2"}]' WHERE `id` = 151;
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]' WHERE `id` IN (159, 167);
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"制度流程","value":"1"},{"label":"信息系统","value":"2"},{"label":"人员服务","value":"3"}]' WHERE `id` = 136;
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"制度建设","value":"1"},{"label":"系统优化","value":"2"},{"label":"人员培训","value":"3"}]' WHERE `id` = 144;
UPDATE `flow_template_field` SET `enum_options` = '[{"label":"整改措施","value":"1"},{"label":"整改期限","value":"2"},{"label":"佐证材料","value":"3"}]' WHERE `id` IN (152, 160, 168);

-- ---------- 四、问题自查（节点25）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`, `field_tips`) VALUES
(178, 5, 25, 1, 'issue_type', '问题类型', 'radio', 8, 1, '[{"label":"流程类","value":"1"},{"label":"系统类","value":"2"},{"label":"服务类","value":"3"},{"label":"其他","value":"4"}]', NULL, '选择发现问题的类别'),
(179, 5, 25, 1, 'discover_time', '发现问题时间', 'date', 9, 1, NULL, '选择发现问题的时间', NULL),
(180, 5, 25, 1, 'duty_person', '责任人', 'text', 10, 1, NULL, '填写问题责任人姓名', NULL),
(181, 5, 25, 1, 'duty_dept', '所属部门', 'text', 11, 0, NULL, '填写责任人所在部门', NULL),
(182, 5, 25, 1, 'issue_desc', '问题详细描述', 'textarea', 12, 1, NULL, '详细描述问题的现象与影响', NULL),
(183, 5, 25, 1, 'urgency', '紧急程度', 'radio', 13, 0, '[{"label":"紧急","value":"1"},{"label":"重要","value":"2"},{"label":"一般","value":"3"}]', NULL, '按影响程度选择');

-- ---------- 五、问题整改（节点26）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `placeholder`, `field_tips`) VALUES
(184, 5, 26, 1, 'rectify_measure', '整改措施', 'textarea', 8, 1, '填写具体整改措施', '分条描述如何整改'),
(185, 5, 26, 1, 'rectify_deadline', '整改完成日期', 'date', 9, 1, '选择计划完成日期', NULL),
(186, 5, 26, 1, 'rectify_person', '整改责任人', 'text', 10, 1, '填写整改责任人姓名', NULL),
(187, 5, 26, 1, 'progress_percent', '整改进度', 'number', 11, 0, '填写进度百分比（0-100）', NULL);

-- ---------- 六、整改审批（节点27）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(188, 5, 27, 1, 'approve_result', '审批结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"不通过","value":"2"}]', '选择审批结果'),
(189, 5, 27, 1, 'approve_opinion', '审批意见', 'textarea', 9, 1, NULL, '填写审批意见'),
(190, 5, 27, 1, 'approver', '审批人', 'text', 10, 0, NULL, '审批人姓名');

-- ---------- 七、下发人审核（节点28）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(191, 5, 28, 1, 'review_result', '审核结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]', '选择审核结果'),
(192, 5, 28, 1, 'review_opinion', '审核意见', 'textarea', 9, 0, NULL, '填写审核意见'),
(193, 5, 28, 1, 'reviewer', '审核人', 'text', 10, 0, NULL, '审核人姓名');

-- ---------- 八、行领导审核（节点29）新增字段 ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `enum_options`, `placeholder`) VALUES
(194, 5, 29, 1, 'final_result', '终审结果', 'radio', 8, 1, '[{"label":"通过","value":"1"},{"label":"驳回","value":"2"}]', '选择终审结果'),
(195, 5, 29, 1, 'final_opinion', '终审意见', 'textarea', 9, 1, NULL, '填写终审意见'),
(196, 5, 29, 1, 'final_remark', '终审批注', 'textarea', 10, 0, NULL, '补充批注说明');

-- ---------- 九、任务基础字段（创建人填写，续 sort 7/8） ----------
INSERT INTO `flow_template_field` (`id`, `template_id`, `node_id`, `field_role`, `field_key`, `field_label`, `field_type`, `sort_num`, `required`, `bind_node_id`, `placeholder`) VALUES
(197, 5, NULL, 1, 'rectify_task_name', '整改任务名称', 'text', 7, 1, NULL, '如：2026年服务问题整改'),
(198, 5, NULL, 1, 'dispatch_unit', '下发单位', 'text', 8, 0, NULL, '如下发整改通知的部门');

-- 填报记录测试数据（5条，跨多日，用于趋势图与统计）
INSERT INTO flow_form_record (task_id, template_id, user_id, record_status, is_draft, submit_time, create_time) VALUES
(1, 1, 1, 1, 0, '2026-07-15 10:00:00', '2026-07-15 10:00:00'),
(1, 1, 1, 1, 0, '2026-07-20 14:30:00', '2026-07-20 14:30:00'),
(1, 1, 1, 1, 0, '2026-07-28 09:15:00', '2026-07-28 09:15:00'),
(1, 1, 1, 1, 1, NULL, '2026-07-30 16:00:00'),
(1, 1, 1, 2, 0, '2026-07-25 11:00:00', '2026-07-25 11:00:00');

-- 为正常记录填充字段值（关联模板4个字段）
INSERT INTO flow_form_data (record_id, field_id, field_key, field_value, create_time)
SELECT r.id, f.id, f.field_key,
  CASE f.field_key
    WHEN 'name' THEN '张三'
    WHEN 'phone' THEN '13800138000'
    WHEN 'edu' THEN '本科'
    WHEN 'material' THEN '[1,2]'
  END,
  r.create_time
FROM flow_form_record r
CROSS JOIN flow_template_field f
WHERE f.template_id = 1 AND r.record_status = 1;

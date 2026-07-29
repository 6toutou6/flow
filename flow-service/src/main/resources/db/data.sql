USE test001;

-- 插入测试数据
SELECT 
    id, user_name, password, doc_number, phone_number, job, work_address,
    room_number, lease_start_date, lease_end_date, lease_status, create_time, update_time
FROM user
WHERE room_number = '101' 
AND lease_status = '在租'
ORDER BY lease_start_date DESC;

-- 先查看将要被更新的记录
SELECT payment_id, user_name, room_amount, batch, total_amount 
FROM rentpayments 
WHERE payment_status = '未支付';

-- 然后执行更新
UPDATE rentpayments 
SET payment_date = null 
WHERE payment_status = '未支付';

-- 最后确认更新结果
SELECT payment_id, user_name, room_amount, batch, total_amount 
FROM rentpayments 
WHERE payment_status = '未支付';
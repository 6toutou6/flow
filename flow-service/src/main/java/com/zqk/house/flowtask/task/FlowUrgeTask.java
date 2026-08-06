package com.zqk.house.flowtask.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zqk.house.flowtask.entity.FlowDispatch;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.entity.FlowTaskDispatch;
import com.zqk.house.flowtask.entity.FlowTaskLog;
import com.zqk.house.flowtask.entity.FlowTaskNode;
import com.zqk.house.flowtask.mapper.FlowDispatchMapper;
import com.zqk.house.flowtask.mapper.FlowTaskDispatchMapper;
import com.zqk.house.flowtask.mapper.FlowTaskLogMapper;
import com.zqk.house.flowtask.mapper.FlowTaskMapper;
import com.zqk.house.flowtask.mapper.FlowTaskNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 周期下发催办定时任务：
 * 每天 09:00 扫描进行中的期次（flow_task_dispatch 挂接了任务且有截止时间），
 * 距截止时间剩余 <= 任务配置的 urge_days 天时，向该期次下每个进行中成员的当前节点处理人
 * 写一条催办日志（log_type=2）。催办仅作后端日志输出体现，不发送任何真实通知。
 * 同一期次同一天只催办一次，避免重复刷日志。
 */
@Component
public class FlowUrgeTask {

    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowDispatchMapper flowDispatchMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTaskLogMapper flowTaskLogMapper;

    @Scheduled(cron = "0 0 9 * * ?")
    public void urgeScan() {
        try {
            Date now = new Date();
            Date todayStart = startOfDay(now);
            List<FlowTaskDispatch> dispatches = flowTaskDispatchMapper.selectList(
                    new LambdaQueryWrapper<FlowTaskDispatch>()
                            .isNotNull(FlowTaskDispatch::getEndTime)
                            .isNotNull(FlowTaskDispatch::getTaskId)
                            .orderByAsc(FlowTaskDispatch::getId));
            for (FlowTaskDispatch dispatch : dispatches) {
                FlowDispatch plan = flowDispatchMapper.selectById(dispatch.getTaskId());
                if (plan == null || plan.getUrgeDays() == null || plan.getUrgeDays() <= 0) continue;
                long remainDays = remainingDays(now, dispatch.getEndTime());
                if (remainDays < 0 || remainDays > plan.getUrgeDays()) continue;
                // 同一天已催办过则跳过
                Long urged = flowTaskLogMapper.selectCount(new LambdaQueryWrapper<FlowTaskLog>()
                        .eq(FlowTaskLog::getDispatchId, dispatch.getId())
                        .eq(FlowTaskLog::getLogType, 2)
                        .ge(FlowTaskLog::getCreateTime, todayStart));
                if (urged != null && urged > 0) continue;
                // 该期次下进行中成员任务
                List<FlowTask> tasks = flowTaskMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getDispatchId, dispatch.getId())
                        .eq(FlowTask::getStatus, 1));
                for (FlowTask task : tasks) {
                    FlowTaskNode node = flowTaskNodeMapper.selectOne(new LambdaQueryWrapper<FlowTaskNode>()
                            .eq(FlowTaskNode::getTaskId, task.getId())
                            .eq(FlowTaskNode::getSubmitStatus, 0)
                            .orderByDesc(FlowTaskNode::getId)
                            .last("LIMIT 1"));
                    FlowTaskLog log = new FlowTaskLog();
                    log.setTaskId(task.getId());
                    log.setDispatchId(dispatch.getId());
                    log.setLogType(2);
                    log.setOperatorId(0L);
                    log.setOperatorName("系统");
                    if (node != null) {
                        log.setTaskNodeId(node.getId());
                        log.setNodeId(node.getNodeId());
                        log.setHandlerUserId(node.getHandlerUserId());
                        log.setContent("截止前" + plan.getUrgeDays() + "天自动催办：任务「" + task.getTaskName() + "」将于 "
                                + fmt(dispatch.getEndTime()) + " 截止，请尽快处理当前节点「" + node.getNodeName() + "」");
                    } else {
                        log.setHandlerUserId(task.getCurrentHandlerId());
                        log.setContent("截止前" + plan.getUrgeDays() + "天自动催办：任务「" + task.getTaskName() + "」将于 "
                                + fmt(dispatch.getEndTime()) + " 截止，请尽快处理");
                    }
                    try {
                        flowTaskLogMapper.insert(log);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            // 定时任务异常不影响主流程
            e.printStackTrace();
        }
    }

    private long remainingDays(Date now, Date end) {
        if (end == null) return Long.MAX_VALUE;
        return (long) Math.ceil((end.getTime() - now.getTime()) / (1000.0 * 60 * 60 * 24));
    }

    private Date startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private String fmt(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(d);
    }
}

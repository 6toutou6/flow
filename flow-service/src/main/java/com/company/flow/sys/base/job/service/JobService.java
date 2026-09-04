package com.company.flow.sys.base.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.flow.sys.flowtask.entity.FlowDispatchConfig;
import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.flowtask.entity.FlowTaskLog;
import com.company.flow.sys.flowtask.entity.FlowTaskNode;
import com.company.flow.sys.flowtask.mapper.FlowDispatchConfigMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskLogMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskNodeMapper;
import com.company.flow.sys.flowtask.service.FlowDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 定时任务统一业务服务：
 * 集中管理三个定时任务的核心逻辑（期次自动下发 / 截止催办 / 待办提醒），
 * 供 job.task 包下各定时器定时触发，也可通过 JobController 手动调用。
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    /** 按用户号查姓名（双冗余入库用） */
    private String userNameOf(String yyytId) {
        if (yyytId == null) return null;
        com.company.flow.sys.base.autuser.entity.User u = userMapper.selectById(yyytId);
        return u == null ? null : u.getUserName();
    }

    @Autowired
    private FlowDispatchService flowDispatchService;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowDispatchConfigMapper flowDispatchConfigMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private com.company.flow.sys.base.autuser.mapper.UserMapper userMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTaskLogMapper flowTaskLogMapper;

    // ==================== 1. 期次自动下发 ====================

    /**
     * 检索启用中非样例的周期任务，若「下一期次窗口开始时间已到」则自动生成并下发出该期次。
     *
     * @return 本次下发的期次数；-1 表示执行异常
     */
    /** 模板创建人字段一致性巡检：对不一致的启用任务向创建人发通知（每天一次，直到任务重新保存同步） */
    public int syncTemplateFieldScan() {
        return flowDispatchService.notifyUnsyncedTemplateTasks();
    }

    public int autoDispatchScan() {
        try {
            int n = flowDispatchService.autoDispatchDuePeriods();
            if (n > 0) {
                log.info("期次自动下发完成：本次下发 {} 个期次", n);
            }
            return n;
        } catch (Exception e) {
            log.error("期次自动下发异常", e);
            return -1;
        }
    }

    // ==================== 2. 截止催办 ====================

    /**
     * 扫描进行中的期次（有截止时间且挂接了任务），距截止剩余天数 &lt;= 下发配置的 urge_days 时，
     * 向该期次下每个进行中成员的当前节点处理人写一条催办日志（log_type=2）。
     * 同一期次同一天只催办一次，避免重复刷日志。
     *
     * @return 写入的催办日志条数；-1 表示执行异常
     */
    public int urgeScan() {
        int count = 0;
        try {
            Date now = new Date();
            Date todayStart = startOfDay(now);
            List<FlowTaskDispatch> dispatches = flowTaskDispatchMapper.selectList(
                    new LambdaQueryWrapper<FlowTaskDispatch>()
                            .isNotNull(FlowTaskDispatch::getEndTime)
                            .isNotNull(FlowTaskDispatch::getTaskId)
                            .orderByAsc(FlowTaskDispatch::getId));
            for (FlowTaskDispatch dispatch : dispatches) {
                // 催办天数从下发配置表读取（FlowDispatch.urgeDays 为冗余字段，selectById 不会回填）
                FlowDispatchConfig config = flowDispatchConfigMapper.selectOne(
                        new LambdaQueryWrapper<FlowDispatchConfig>()
                                .eq(FlowDispatchConfig::getTaskId, dispatch.getTaskId()));
                if (config == null || config.getUrgeDays() == null || config.getUrgeDays() <= 0) {
                    continue;
                }
                Date endTime = dispatch.getEndTime();
                if (endTime == null) {
                    continue;
                }
                // 催办时间 = 截止时间 - 提前催办天数（计算得出，不落库）
                Date urgeTime = addDays(endTime, -config.getUrgeDays());
                if (now.before(urgeTime)) {
                    continue; // 未到催办时间
                }
                if (now.after(endTime)) {
                    continue; // 已截止不再催办
                }
                // 同一天已催办过则跳过
                Long urged = flowTaskLogMapper.selectCount(new LambdaQueryWrapper<FlowTaskLog>()
                        .eq(FlowTaskLog::getDispatchId, dispatch.getId())
                        .eq(FlowTaskLog::getLogType, 2)
                        .ge(FlowTaskLog::getCreateTime, todayStart));
                if (urged != null && urged > 0) {
                    continue;
                }
                // 该期次下进行中的成员任务
                List<FlowTask> tasks = flowTaskMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getDispatchId, dispatch.getId())
                        .eq(FlowTask::getStatus, "进行中"));
                for (FlowTask task : tasks) {
                    FlowTaskNode node = flowTaskNodeMapper.selectOne(new LambdaQueryWrapper<FlowTaskNode>()
                            .eq(FlowTaskNode::getTaskId, task.getId())
                            .eq(FlowTaskNode::getSubmitStatus, 0)
                            .orderByDesc(FlowTaskNode::getId)
                            .last("LIMIT 1"));
                    FlowTaskLog l = new FlowTaskLog();
                    l.setTaskId(task.getId());
                    l.setDispatchId(dispatch.getId());
                    l.setLogType(2);
                    l.setOperatorId("0");
                    l.setOperatorName("系统");
                    if (node != null) {
                        l.setTaskNodeId(node.getId());
                        l.setNodeId(node.getNodeId());
                        l.setHandlerUserId(node.getHandlerUserId());
                        l.setHandlerUserName(node.getHandlerUserName());
                        l.setContent("截止前" + config.getUrgeDays() + "天自动催办：任务「" + task.getTaskName() + "」将于 "
                                + fmt(dispatch.getEndTime()) + " 截止，请尽快处理当前节点「" + node.getNodeName() + "」");
                    } else {
                        l.setHandlerUserId(task.getCurrentHandlerId());
                        l.setHandlerUserName(task.getCurrentHandlerId() == null ? null : userNameOf(task.getCurrentHandlerId()));
                        l.setContent("截止前" + config.getUrgeDays() + "天自动催办：任务「" + task.getTaskName() + "」将于 "
                                + fmt(dispatch.getEndTime()) + " 截止，请尽快处理");
                    }
                    try {
                        flowTaskLogMapper.insert(l);
                        count++;
                    } catch (Exception ignored) {
                    }
                }
            }
            if (count > 0) {
                log.info("截止催办完成：共写入 {} 条催办日志", count);
            }
        } catch (Exception e) {
            log.error("截止催办异常", e);
            return -1;
        }
        return count;
    }

    // ==================== 3. 待办提醒 ====================

    /**
     * 检索所有待处理节点（submit_status=0 且已指定处理人），向当前处理人写一条提醒日志（log_type=3）。
     * 同一节点同一天只提醒一次，避免重复刷日志。
     *
     * @return 写入的提醒日志条数；-1 表示执行异常
     */
    public int remindScan() {
        int count = 0;
        try {
            Date todayStart = startOfDay(new Date());
            List<FlowTaskNode> nodes = flowTaskNodeMapper.selectList(
                    new LambdaQueryWrapper<FlowTaskNode>()
                            .eq(FlowTaskNode::getSubmitStatus, 0)
                            .isNotNull(FlowTaskNode::getHandlerUserId));
            for (FlowTaskNode node : nodes) {
                // 同一节点同一天已提醒过则跳过
                Long reminded = flowTaskLogMapper.selectCount(new LambdaQueryWrapper<FlowTaskLog>()
                        .eq(FlowTaskLog::getTaskNodeId, node.getId())
                        .eq(FlowTaskLog::getLogType, 3)
                        .ge(FlowTaskLog::getCreateTime, todayStart));
                if (reminded != null && reminded > 0) {
                    continue;
                }
                FlowTask task = flowTaskMapper.selectById(node.getTaskId());
                FlowTaskLog l = new FlowTaskLog();
                l.setTaskId(node.getTaskId());
                l.setDispatchId(task != null ? task.getDispatchId() : null);
                l.setTaskNodeId(node.getId());
                l.setNodeId(node.getNodeId());
                l.setLogType(3);
                l.setHandlerUserId(node.getHandlerUserId());
                l.setHandlerUserName(node.getHandlerUserName());
                l.setOperatorId("0");
                l.setOperatorName("系统");
                l.setContent("待办提醒：任务「" + (task != null ? task.getTaskName() : node.getTaskId())
                        + "」当前节点「" + node.getNodeName() + "」等待处理，请尽快完成");
                try {
                    flowTaskLogMapper.insert(l);
                    count++;
                } catch (Exception ignored) {
                }
            }
            if (count > 0) {
                log.info("待办提醒完成：共提醒 {} 个待处理节点", count);
            }
        } catch (Exception e) {
            log.error("待办提醒异常", e);
            return -1;
        }
        return count;
    }

    // ==================== 工具方法 ====================

    private Date addDays(Date d, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
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

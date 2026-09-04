package com.company.flow.sys.base.job.task;

import com.company.flow.sys.base.job.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：待办提醒（每天 10:00）。
 * 检索所有待处理节点，向当前处理人写提醒日志（log_type=3），提示尽快处理。
 * 可调用 POST /job/remind 手动触发。
 */
@Component
public class RemindNodeJob {

    private static final Logger log = LoggerFactory.getLogger(RemindNodeJob.class);

    @Autowired
    private JobService jobService;

    @Scheduled(cron = "0 0 10 * * ?")
    public void run() {
        log.info("[定时任务] 开始执行待办提醒");
        jobService.remindScan();
    }
}

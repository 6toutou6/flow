package com.company.flow.sys.base.job.task;

import com.company.flow.sys.base.job.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：截止催办（每天 09:00）。
 * 距期次截止时间剩余天数小于等于配置的催办天数时，向各成员当前节点处理人写催办日志（log_type=2）。
 * 可调用 POST /job/urge 手动触发。
 */
@Component
public class UrgeJob {

    private static final Logger log = LoggerFactory.getLogger(UrgeJob.class);

    @Autowired
    private JobService jobService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void run() {
        log.info("[定时任务] 开始执行截止催办");
        jobService.urgeScan();
    }
}

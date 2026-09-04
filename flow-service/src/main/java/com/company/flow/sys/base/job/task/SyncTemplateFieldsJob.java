package com.company.flow.sys.base.job.task;

import com.company.flow.sys.base.job.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：模板创建人字段一致性巡检（每天 08:05）。
 * 找出「模板创建人填写字段与任务固化配置不一致」的启用任务，向任务创建人推送机器人通知，
 * 每天提醒一次直到创建人在任务管理中重新保存（同步后不再命中）。
 * 可调用 POST /job/template-field-sync 手动触发。
 */
@Component
public class SyncTemplateFieldsJob {

    private static final Logger log = LoggerFactory.getLogger(SyncTemplateFieldsJob.class);

    @Autowired
    private JobService jobService;

    @Scheduled(cron = "0 5 8 * * ?")
    public void run() {
        log.info("[定时任务] 开始执行模板创建人字段一致性巡检");
        try {
            int n = jobService.syncTemplateFieldScan();
            log.info("[定时任务] 模板字段一致性巡检完成，通知 {} 个任务创建人", n);
        } catch (Exception e) {
            log.error("[定时任务] 模板字段一致性巡检异常", e);
        }
    }
}

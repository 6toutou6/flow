package com.zqk.house.job.task;

import com.zqk.house.job.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：期次自动下发（每天 08:00）。
 * 检索启用中非样例的周期任务，若「下一期次窗口开始时间已到」则自动生成并下发出该期次。
 * 可调用 POST /job/auto-dispatch 手动触发。
 */
@Component
public class PeriodAutoDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(PeriodAutoDispatchJob.class);

    @Autowired
    private JobService jobService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void run() {
        log.info("[定时任务] 开始执行期次自动下发");
        jobService.autoDispatchScan();
    }
}

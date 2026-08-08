package com.zqk.house.flowtask.task;

import com.zqk.house.flowtask.service.FlowDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 期次自动下发定时任务：
 * 每天 08:00 检索启用中非样例的周期任务，若「下一期次窗口开始时间已到」则自动生成并下发出该期次
 * （复用 FlowDispatchService.autoDispatchDuePeriods，内部 synchronized 防重复）。
 */
@Component
public class PeriodAutoDispatchTask {

    private static final Logger log = LoggerFactory.getLogger(PeriodAutoDispatchTask.class);

    @Autowired
    private FlowDispatchService flowDispatchService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void autoDispatchScan() {
        try {
            int n = flowDispatchService.autoDispatchDuePeriods();
            if (n > 0) {
                log.info("期次自动下发完成：本次下发 {} 个期次", n);
            }
        } catch (Exception e) {
            log.error("期次自动下发异常", e);
        }
    }
}

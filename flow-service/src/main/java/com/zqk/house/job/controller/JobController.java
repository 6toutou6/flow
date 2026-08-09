package com.zqk.house.job.controller;

import com.zqk.house.job.service.JobService;
import com.zqk.house.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务手动触发接口：
 * 三个定时任务（期次自动下发 / 截止催办 / 待办提醒）除定时器自动执行外，
 * 均可通过本接口手动触发一次，调用的是与定时器相同的 JobService 方法。
 */
@RestController
@RequestMapping("/job")
public class JobController {

    @Autowired
    private JobService jobService;

    /** 手动触发：期次自动下发 */
    @PostMapping("/auto-dispatch")
    public Result<Integer> autoDispatch() {
        int n = jobService.autoDispatchScan();
        return n >= 0
                ? Result.success(n > 0 ? "已自动下发 " + n + " 个期次" : "当前无到期的期次", n)
                : Result.fail("期次自动下发执行异常");
    }

    /** 手动触发：截止催办 */
    @PostMapping("/urge")
    public Result<Integer> urge() {
        int n = jobService.urgeScan();
        return n >= 0
                ? Result.success("截止催办完成，共写入 " + n + " 条催办日志", n)
                : Result.fail("截止催办执行异常");
    }

    /** 手动触发：待办提醒 */
    @PostMapping("/remind")
    public Result<Integer> remind() {
        int n = jobService.remindScan();
        return n >= 0
                ? Result.success("待办提醒完成，共提醒 " + n + " 个节点", n)
                : Result.fail("待办提醒执行异常");
    }
}

package com.zqk.house.flowdata.controller;

import com.zqk.house.flowdata.service.FlowDataService;
import com.zqk.house.flowdata.vo.DataStatsVO;
import com.zqk.house.flowdata.vo.DashboardVO;
import com.zqk.house.flowdata.vo.FormRecordDetailVO;
import com.zqk.house.flowdata.vo.FormRecordListVO;
import com.zqk.house.flowdata.vo.FormRecordQueryForm;
import com.zqk.house.flowdata.vo.PersonRecordVO;
import com.zqk.house.flowdata.vo.PersonSubmitVO;
import com.zqk.house.flowdata.vo.TrendPointVO;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flow-data")
@CrossOrigin
public class FlowDataController {

    @Autowired
    private FlowDataService flowDataService;

    @PostMapping("/list")
    public Result<PageResult<FormRecordListVO>> list(@RequestBody FormRecordQueryForm form) {
        return Result.success("获取成功", flowDataService.getPage(form));
    }

    @GetMapping("/record/{id}")
    public Result<FormRecordDetailVO> detail(@PathVariable Long id) {
        FormRecordDetailVO vo = flowDataService.getDetail(id);
        return vo != null ? Result.success("获取成功", vo) : Result.notFound("记录不存在");
    }

    @GetMapping("/stats")
    public Result<DataStatsVO> stats() {
        return Result.success("获取成功", flowDataService.getStats());
    }

    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "30") int days) {
        return Result.success("获取成功", flowDataService.getTrend(days));
    }

    // ==================== 按人员展示 ====================

    /** 人员提交汇总列表（分页，姓名/工号过滤；taskId 非空时仅统计该任务下人员） */
    @GetMapping("/person-list")
    public Result<PageResult<PersonSubmitVO>> personList(@RequestParam(required = false) String name,
                                                         @RequestParam(required = false) Long taskId,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int limit) {
        return Result.success("获取成功", flowDataService.getPersonPage(name, taskId, page, limit));
    }

    /** 某人历史提交记录（分页；taskId 非空时仅统计该任务下提交） */
    @GetMapping("/person-records")
    public Result<PageResult<PersonRecordVO>> personRecords(@RequestParam Long userId,
                                                            @RequestParam(required = false) Long taskId,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int limit) {
        return Result.success("获取成功", flowDataService.getPersonRecords(userId, taskId, page, limit));
    }

    // ==================== 数据展示页 ====================

    /** 数据展示页聚合数据（统计卡 + 各图表） */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(@RequestParam(defaultValue = "day") String trendType,
                                         @RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        return Result.success("获取成功", flowDataService.getDashboard(trendType, startDate, endDate));
    }
}

package com.company.flow.sys.flowdata.controller;

import com.company.flow.sys.flowdata.service.FlowDataService;
import com.company.flow.sys.flowdata.vo.DataStatsVO;
import com.company.flow.sys.flowdata.vo.DashboardVO;
import com.company.flow.sys.flowdata.vo.FormRecordDetailVO;
import com.company.flow.sys.flowdata.vo.FormRecordListVO;
import com.company.flow.sys.flowdata.vo.FormRecordQueryForm;
import com.company.flow.sys.flowdata.vo.PersonNodeVO;
import com.company.flow.sys.flowdata.vo.PersonSubmitVO;
import com.company.flow.sys.flowdata.vo.TrendPointVO;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.result.Result;
import com.company.flow.sys.base.util.ParamUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Result<FormRecordDetailVO> detail(@PathVariable String id) {
        FormRecordDetailVO vo = flowDataService.getDetail(id);
        return vo != null ? Result.success("获取成功", vo) : Result.notFound("记录不存在");
    }

    @PostMapping("/stats")
    public Result<DataStatsVO> stats() {
        return Result.success("获取成功", flowDataService.getStats());
    }

    @GetMapping("/trend/{days}")
    public Result<List<TrendPointVO>> trend(@PathVariable int days) {
        return Result.success("获取成功", flowDataService.getTrend(days));
    }

    // ==================== 按人员展示 ====================

    /** 人员提交汇总列表（分页，姓名/工号过滤；taskId 非空时仅统计该任务下人员） */
    @PostMapping("/person-list")
    public Result<PageResult<PersonSubmitVO>> personList(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> p = body == null ? new HashMap<>() : body;
        return Result.success("获取成功", flowDataService.getPersonPage(
                ParamUtil.str(p.get("name")),
                ParamUtil.str(p.get("taskId")),
                ParamUtil.intv(p.get("page"), 1),
                ParamUtil.intv(p.get("limit"), 10)));
    }

    /** 某人参与的任务链全部节点（分页；taskId 非空时仅返回该任务下的链） */
    @PostMapping("/person-records")
    public Result<PageResult<PersonNodeVO>> personRecords(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> p = body == null ? new HashMap<>() : body;
        return Result.success("获取成功", flowDataService.getPersonRecords(
                ParamUtil.str(p.get("userId")),
                ParamUtil.str(p.get("taskId")),
                ParamUtil.intv(p.get("page"), 1),
                ParamUtil.intv(p.get("limit"), 10)));
    }

    // ==================== 数据展示页 ====================

    /** 数据展示页聚合数据（统计卡 + 各图表） */
    @PostMapping("/dashboard")
    public Result<DashboardVO> dashboard(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> p = body == null ? new HashMap<>() : body;
        String trendType = ParamUtil.str(p.get("trendType"));
        return Result.success("获取成功", flowDataService.getDashboard(
                trendType == null || trendType.isEmpty() ? "day" : trendType,
                ParamUtil.str(p.get("startDate")),
                ParamUtil.str(p.get("endDate"))));
    }
}

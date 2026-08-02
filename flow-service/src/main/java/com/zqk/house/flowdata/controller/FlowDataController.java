package com.zqk.house.flowdata.controller;

import com.zqk.house.flowdata.service.FlowDataService;
import com.zqk.house.flowdata.vo.DataStatsVO;
import com.zqk.house.flowdata.vo.FormRecordDetailVO;
import com.zqk.house.flowdata.vo.FormRecordListVO;
import com.zqk.house.flowdata.vo.FormRecordQueryForm;
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
}

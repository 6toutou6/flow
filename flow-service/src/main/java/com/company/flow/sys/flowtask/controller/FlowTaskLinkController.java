package com.company.flow.sys.flowtask.controller;

import com.company.flow.sys.base.result.Result;
import com.company.flow.sys.flowtask.entity.FlowTaskLink;
import com.company.flow.sys.flowtask.service.FlowTaskLinkService;
import com.company.flow.sys.flowtask.vo.TaskLinkVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务关联（汇总 ← 收集）
 */
@RestController
@RequestMapping("/flow-task-link")
public class FlowTaskLinkController {

    @Autowired
    private FlowTaskLinkService flowTaskLinkService;

    /** 建立关联：配置级（sourceDispatchId）或成员级（sourceTaskId） */
    @PostMapping
    public Result<TaskLinkVO> create(@RequestBody FlowTaskLink link) {
        try {
            return Result.success("关联成功", flowTaskLinkService.create(link));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 按来源查询（管理端按期次 periodId 或任务配置 dispatchId；taskId 为兼容保留） */
    @GetMapping("/by-source")
    public Result<List<TaskLinkVO>> listBySource(@RequestParam(required = false) String taskId,
                                                 @RequestParam(required = false) String dispatchId,
                                                 @RequestParam(required = false) String periodId) {
        return Result.success("获取成功", flowTaskLinkService.listBySource(taskId, dispatchId, periodId));
    }

    /** 按目标反查（收到任务侧查「被哪些期次关联」传 taskId；dispatchId/periodId 为兼容保留） */
    @GetMapping("/by-target")
    public Result<List<TaskLinkVO>> listByTarget(@RequestParam(required = false) String taskId,
                                                 @RequestParam(required = false) String dispatchId,
                                                 @RequestParam(required = false) String periodId) {
        return Result.success("获取成功", flowTaskLinkService.listByTarget(taskId, dispatchId, periodId));
    }

    /** 解除关联（仅创建人或超管） */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable String id) {
        try {
            flowTaskLinkService.remove(id);
            return Result.success("已解除关联", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}

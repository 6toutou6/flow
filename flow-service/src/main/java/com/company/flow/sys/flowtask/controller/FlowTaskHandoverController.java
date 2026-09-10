package com.company.flow.sys.flowtask.controller;

import com.company.flow.sys.base.result.Result;
import com.company.flow.sys.flowtask.entity.FlowTaskHandover;
import com.company.flow.sys.flowtask.service.FlowTaskHandoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务交接（处理人发起，创建人同部门部门管理员审批）
 */
@RestController
@RequestMapping("/flow-task-handover")
public class FlowTaskHandoverController {

    @Autowired
    private FlowTaskHandoverService flowTaskHandoverService;

    /** 发起交接申请 */
    @PostMapping
    public Result<FlowTaskHandover> apply(@RequestBody FlowTaskHandover req) {
        try {
            return Result.success("交接申请已提交，等待审批", flowTaskHandoverService.apply(req));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 我发起的交接申请 */
    @GetMapping("/mine")
    public Result<List<FlowTaskHandover>> mine() {
        return Result.success("获取成功", flowTaskHandoverService.listMine());
    }

    /** 待我审批 */
    @GetMapping("/pending")
    public Result<List<FlowTaskHandover>> pending() {
        return Result.success("获取成功", flowTaskHandoverService.listPendingForApprover());
    }

    /** 待我审批数量（角标） */
    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.success("获取成功", flowTaskHandoverService.pendingCountForApprover());
    }

    /** 某任务配置下我相关的交接记录（仅我是交接人或接手人） */
    @GetMapping("/by-dispatch")
    public Result<List<FlowTaskHandover>> byDispatch(@RequestParam String dispatchId) {
        return Result.success("获取成功", flowTaskHandoverService.listByDispatch(dispatchId));
    }

    /** 我相关的交接记录（仅我是交接人或接手人，只读查看） */
    @GetMapping("/records")
    public Result<List<FlowTaskHandover>> records() {
        return Result.success("获取成功", flowTaskHandoverService.listMineRelated());
    }

    /** 本部门交接记录（部门管理员审批范围内，含全部状态） */
    @GetMapping("/approver-records")
    public Result<List<FlowTaskHandover>> approverRecords() {
        return Result.success("获取成功", flowTaskHandoverService.listApproverRecords());
    }

    /** 审批通过（可覆盖是否同步任务配置名单） */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            Integer syncMember = null;
            if (body != null && body.get("syncMember") != null) {
                syncMember = Integer.valueOf(String.valueOf(body.get("syncMember")));
            }
            flowTaskHandoverService.approve(id, syncMember);
            return Result.success("已通过，交接完成", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 审批拒绝 */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body == null ? null : body.get("reason");
            flowTaskHandoverService.reject(id, reason);
            return Result.success("已拒绝", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}

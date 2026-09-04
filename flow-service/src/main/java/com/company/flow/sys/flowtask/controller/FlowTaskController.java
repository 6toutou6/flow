package com.company.flow.sys.flowtask.controller;

import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.entity.FlowTaskLog;
import com.company.flow.sys.flowtask.entity.FlowTaskQueryForm;
import com.company.flow.sys.flowtask.service.FlowTaskService;
import com.company.flow.sys.flowtask.vo.AddHandlersDTO;
import com.company.flow.sys.flowtask.vo.MyTodoStatsVO;
import com.company.flow.sys.flowtask.vo.MyTodoTaskVO;
import com.company.flow.sys.flowtask.vo.MyTodoVO;
import com.company.flow.sys.flowtask.vo.NodeSubmitDTO;
import com.company.flow.sys.flowtask.vo.TaskDetailVO;
import com.company.flow.sys.flowtask.vo.TaskGroupVO;
import com.company.flow.sys.flowtask.vo.TaskMemberVO;
import com.company.flow.sys.flowtask.vo.TaskProgressVO;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.result.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flow-task")
@CrossOrigin
public class FlowTaskController {

    @Autowired
    private FlowTaskService flowTaskService;

    @PostMapping("/list")
    public Result<PageResult<TaskGroupVO>> list(@RequestBody FlowTaskQueryForm form) {
        return Result.success("获取成功", flowTaskService.getPage(form));
    }

    /** 任务组详情：组头 + 全部成员（数据后台 level 2） */
    @GetMapping("/dispatch/{dispatchId}")
    public Result<TaskGroupVO> dispatchDetail(@PathVariable String dispatchId) {
        TaskGroupVO vo = flowTaskService.getDispatchDetail(dispatchId);
        return vo != null ? Result.success("获取成功", vo) : Result.notFound("任务组不存在");
    }

    /** 任务组成员分页查询（姓名/部门/状态过滤 + 分页） */
    @GetMapping("/dispatch/{dispatchId}/members")
    public Result<PageResult<TaskMemberVO>> members(@PathVariable String dispatchId,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer limit,
                                                    @RequestParam(required = false) String name,
                                                    @RequestParam(required = false) String dept,
                                                    @RequestParam(required = false) String status) {
        return Result.success("获取成功", flowTaskService.getMembersPage(dispatchId, page, limit, name, dept, status));
    }

    @GetMapping("/{id}")
    public Result<TaskDetailVO> detail(@PathVariable String id) {
        TaskDetailVO vo = flowTaskService.getDetail(id);
        return vo != null ? Result.success("获取成功", vo) : Result.notFound("任务不存在");
    }

    /** 新增人员：在期次下为每个新增人员创建独立成员任务 */
    @PostMapping("/add-handlers")
    public Result<Integer> addHandlers(@RequestBody AddHandlersDTO dto) {
        try {
            int count = flowTaskService.addHandlers(dto.getDispatchId(), dto.getHandlerIds());
            return Result.success("成功新增 " + count + " 名处理人", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 删除主任务（任务组）：仅当组内无人员时才允许 */
    @DeleteMapping("/dispatch/{dispatchId}")
    public Result<Void> deleteDispatch(@PathVariable String dispatchId) {
        try {
            return flowTaskService.deleteDispatch(dispatchId) ? Result.success("删除成功") : Result.fail("删除失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 删除任务中的某个处理人（连同其所有提交记录） */
    @DeleteMapping("/remove-handler")
    public Result<Integer> removeHandler(@RequestParam String taskId, @RequestParam String handlerUserId) {
        try {
            int count = flowTaskService.removeHandler(taskId, handlerUserId);
            return Result.success("已删除该处理人的 " + count + " 条记录", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 节点提交并流转 */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody NodeSubmitDTO dto) {
        try {
            String recordId = flowTaskService.submit(dto);
            return Result.success("提交成功", recordId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 暂存（保存草稿，不校验必填、不流转；下次打开处理弹窗自动回填） */
    @PostMapping("/save-draft")
    public Result<Void> saveDraft(@RequestBody NodeSubmitDTO dto) {
        try {
            flowTaskService.saveDraft(dto);
            return Result.success("暂存成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 我的待办 */
    @PostMapping("/my-todo")
    public Result<PageResult<MyTodoVO>> myTodo(@RequestBody FlowTaskQueryForm form) {
        return Result.success("获取成功", flowTaskService.myTodo(form.getPage(), form.getLimit()));
    }

    /** 我的任务（任务→期次 两级展示）：任务级分页，任务下按期次分组待办节点 */
    @PostMapping("/my-todo-grouped")
    public Result<PageResult<MyTodoTaskVO>> myTodoGrouped(@RequestBody FlowTaskQueryForm form) {
        Integer status = null;
        if (form.getStatus() != null && !form.getStatus().trim().isEmpty()) {
            status = Integer.valueOf(form.getStatus());
        }
        return Result.success("获取成功", flowTaskService.myTodoGrouped(form.getPage(), form.getLimit(), form.getTaskName(), status));
    }

    /** 我的任务统计（统计卡） */
    @GetMapping("/my-todo-stats")
    public Result<MyTodoStatsVO> myTodoStats() {
        return Result.success("获取成功", flowTaskService.myTodoStats());
    }

    /** 任务流转进度 */
    @GetMapping("/progress/{taskId}")
    public Result<List<TaskProgressVO>> progress(@PathVariable String taskId) {
        return Result.success("获取成功", flowTaskService.taskProgress(taskId));
    }

    /** 催办：向成员任务（进行中）发送催办通知并记录日志 */
    @PostMapping("/urge/{taskId}")
    public Result<Void> urge(@PathVariable String taskId) {
        try {
            return flowTaskService.urgeTask(taskId) ? Result.success("催办通知已发送") : Result.fail("催办失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 批量催办：对多个成员任务发送催办通知并记录日志 */
    @PostMapping("/urge-batch")
    public Result<Integer> urgeBatch(@RequestBody Map<String, List<String>> body) {
        try {
            int count = flowTaskService.urgeTaskBatch(body == null ? null : body.get("taskIds"));
            return Result.success("催办通知已发送", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 批量删除成员任务（级联清理节点/表单/附件） */
    @PostMapping("/delete-batch")
    public Result<Integer> deleteBatch(@RequestBody Map<String, List<String>> body) {
        try {
            int count = flowTaskService.deleteBatch(body == null ? null : body.get("taskIds"));
            return Result.success("删除成功", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 任务的流转/催办日志 */
    @GetMapping("/logs/{taskId}")
    public Result<List<FlowTaskLog>> logs(@PathVariable String taskId) {
        return Result.success("获取成功", flowTaskService.getTaskLogs(taskId));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody FlowTask task) {
        return flowTaskService.update(task) ? Result.success("更新成功") : Result.fail("更新失败");
    }

    @PutMapping("/end/{id}")
    public Result<Void> end(@PathVariable String id) {
        return flowTaskService.endTask(id) ? Result.success("操作成功") : Result.fail("操作失败");
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable String id) {
        return flowTaskService.cancelTask(id) ? Result.success("操作成功") : Result.fail("操作失败");
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable String id) {
        return flowTaskService.delete(id) ? Result.success("删除成功") : Result.fail("删除失败");
    }
}

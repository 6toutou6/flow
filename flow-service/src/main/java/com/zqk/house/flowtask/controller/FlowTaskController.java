package com.zqk.house.flowtask.controller;

import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.entity.FlowTaskQueryForm;
import com.zqk.house.flowtask.service.FlowTaskService;
import com.zqk.house.flowtask.vo.AddHandlersDTO;
import com.zqk.house.flowtask.vo.MyTodoVO;
import com.zqk.house.flowtask.vo.NodeSubmitDTO;
import com.zqk.house.flowtask.vo.TaskCreateDTO;
import com.zqk.house.flowtask.vo.TaskDetailVO;
import com.zqk.house.flowtask.vo.TaskGroupVO;
import com.zqk.house.flowtask.vo.TaskProgressVO;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<TaskGroupVO> dispatchDetail(@PathVariable Long dispatchId) {
        TaskGroupVO vo = flowTaskService.getDispatchDetail(dispatchId);
        return vo != null ? Result.success("获取成功", vo) : Result.notFound("任务组不存在");
    }

    @GetMapping("/{id}")
    public Result<TaskDetailVO> detail(@PathVariable Long id) {
        TaskDetailVO vo = flowTaskService.getDetail(id);
        return vo != null ? Result.success("获取成功", vo) : Result.notFound("任务不存在");
    }

    @PostMapping("/create")
    public Result<Integer> create(@RequestBody TaskCreateDTO dto) {
        try {
            int count = flowTaskService.create(dto);
            return Result.success("成功下发 " + count + " 个任务", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 临时增加处理人（加入当前节点并行处理） */
    @PostMapping("/add-handlers")
    public Result<Integer> addHandlers(@RequestBody AddHandlersDTO dto) {
        try {
            int count = flowTaskService.addHandlers(dto.getTaskId(), dto.getHandlerIds());
            return Result.success("成功新增 " + count + " 名处理人", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 删除任务中的某个处理人（连同其所有提交记录） */
    @DeleteMapping("/remove-handler")
    public Result<Integer> removeHandler(@RequestParam Long taskId, @RequestParam Long handlerUserId) {
        try {
            int count = flowTaskService.removeHandler(taskId, handlerUserId);
            return Result.success("已删除该处理人的 " + count + " 条记录", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 节点提交并流转 */
    @PostMapping("/submit")
    public Result<Long> submit(@RequestBody NodeSubmitDTO dto) {
        try {
            Long recordId = flowTaskService.submit(dto);
            return Result.success("提交成功", recordId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 我的待办 */
    @PostMapping("/my-todo")
    public Result<PageResult<MyTodoVO>> myTodo(@RequestBody FlowTaskQueryForm form) {
        return Result.success("获取成功", flowTaskService.myTodo(form.getPage(), form.getLimit()));
    }

    /** 任务流转进度 */
    @GetMapping("/progress/{taskId}")
    public Result<List<TaskProgressVO>> progress(@PathVariable Long taskId) {
        return Result.success("获取成功", flowTaskService.taskProgress(taskId));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody FlowTask task) {
        return flowTaskService.update(task) ? Result.success("更新成功") : Result.fail("更新失败");
    }

    @PutMapping("/end/{id}")
    public Result<Void> end(@PathVariable Long id) {
        return flowTaskService.endTask(id) ? Result.success("操作成功") : Result.fail("操作失败");
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        return flowTaskService.cancelTask(id) ? Result.success("操作成功") : Result.fail("操作失败");
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return flowTaskService.delete(id) ? Result.success("删除成功") : Result.fail("删除失败");
    }
}

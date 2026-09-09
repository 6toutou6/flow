package com.company.flow.sys.flowtask.controller;

import com.company.flow.sys.flowtask.entity.FlowDispatch;
import com.company.flow.sys.flowtask.entity.FlowDispatchConfigTemplate;
import com.company.flow.sys.flowtask.service.FlowDispatchConfigTemplateService;
import com.company.flow.sys.flowtask.service.FlowDispatchService;
import com.company.flow.sys.flowtask.vo.ConfigTemplateStatsVO;
import com.company.flow.sys.flowtask.vo.DispatchStatsVO;
import com.company.flow.sys.flowtask.vo.DueDispatchVO;
import com.company.flow.sys.flowtask.vo.PeriodGenerateVO;
import com.company.flow.sys.flowtask.vo.PeriodPreviewVO;
import com.company.flow.sys.flowtask.vo.TaskMemberInfoVO;
import com.company.flow.sys.flowtask.vo.TaskSaveDTO;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/flow-dispatch")
@CrossOrigin
public class FlowDispatchController {

    @Autowired
    private FlowDispatchService flowDispatchService;

    @Autowired
    private FlowDispatchConfigTemplateService configTemplateService;

    // ==================== 下发配置模板 ====================

    /** 下发配置模板列表（新建任务时可拉取复用；支持样例/我的/周期/创建人/更新时间范围过滤，可叠加） */
    @GetMapping("/config-template/list")
    public Result<List<FlowDispatchConfigTemplate>> configTemplateList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer sample,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) Integer cycleType,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String updateStart,
            @RequestParam(required = false) String updateEnd) {
        return Result.success("获取成功", configTemplateService.list(keyword, sample, mine, cycleType, creatorName, updateStart, updateEnd));
    }

    /** 下发配置模板页统计卡 */
    @GetMapping("/config-template/stats")
    public Result<ConfigTemplateStatsVO> configTemplateStats() {
        return Result.success("获取成功", configTemplateService.stats());
    }

    /** 下发配置模板详情（编辑回填） */
    @GetMapping("/config-template/{id}")
    public Result<FlowDispatchConfigTemplate> configTemplateDetail(@PathVariable String id) {
        try {
            FlowDispatchConfigTemplate tpl = configTemplateService.getById(id);
            return tpl != null ? Result.success("获取成功", tpl) : Result.notFound("配置模板不存在");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 新增或更新下发配置模板 */
    @PostMapping("/config-template/save")
    public Result<Void> configTemplateSave(@RequestBody FlowDispatchConfigTemplate tpl) {
        try {
            if (!StringUtils.hasText(tpl.getConfigName())) {
                return Result.fail("配置名称不能为空");
            }
            if (tpl.getCycleType() == null) {
                return Result.fail("请选择周期类型");
            }
            if (tpl.getDeadlineDays() == null || tpl.getDeadlineDays() <= 0) {
                return Result.fail("请填写截止天数");
            }
            boolean isNew = tpl.getId() == null;
            configTemplateService.save(tpl);
            return Result.success(isNew ? "保存成功" : "更新成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 删除下发配置模板（不影响已拉取到任务上的配置） */
    @DeleteMapping("/config-template/delete/{id}")
    public Result<Void> configTemplateDelete(@PathVariable String id) {
        try {
            return configTemplateService.delete(id) ? Result.success("删除成功") : Result.fail("删除失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 设置/取消下发配置模板样例：仅超管可操作（样例公共可见、不可改） */
    @PutMapping("/config-template/sample/{id}")
    public Result<Void> configTemplateToggleSample(@PathVariable String id) {
        try {
            return configTemplateService.toggleSample(id) ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 复制下发配置模板（样例可复制；复制产物归属当前部门管理员并转为普通配置） */
    @PostMapping("/config-template/copy/{id}")
    public Result<FlowDispatchConfigTemplate> configTemplateCopy(@PathVariable String id) {
        try {
            return Result.success("复制成功", configTemplateService.copy(id));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 任务分页列表（含期次数、人员数） */
    @GetMapping("/list")
    public Result<PageResult<FlowDispatch>> list(@RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer limit,
                                                 @RequestParam(required = false) String taskName,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Integer sample,
                                                 @RequestParam(required = false) String progress,
                                                 @RequestParam(required = false) String creatorName,
                                                 @RequestParam(required = false) String createStart,
                                                 @RequestParam(required = false) String createEnd) {
        return Result.success("获取成功", flowDispatchService.getPage(page, limit, taskName, status, sample, progress, creatorName, createStart, createEnd));
    }

    /** 任务管理页统计卡 */
    @GetMapping("/stats")
    public Result<DispatchStatsVO> stats() {
        return Result.success("获取成功", flowDispatchService.getStats());
    }

    /** 启用中的任务列表 */
    @GetMapping("/enabled-list")
    public Result<List<FlowDispatch>> enabledList() {
        return Result.success("获取成功", flowDispatchService.getEnabledList());
    }

    /** 期次预览：按任务周期 + 是否立即下发，计算期次序号/默认期次名/开始截止时间 */
    @GetMapping("/preview-period")
    public Result<PeriodPreviewVO> previewPeriod(@RequestParam(required = false) String taskId,
                                                 @RequestParam(required = false, defaultValue = "true") Boolean immediate) {
        return Result.success("获取成功", flowDispatchService.previewPeriod(taskId, Boolean.TRUE.equals(immediate)));
    }

    /** 检索当前是否有任务的期次应下发（启用中非样例周期任务，到期待下发列表） */
    @GetMapping("/check-due")
    public Result<List<DueDispatchVO>> checkDue() {
        return Result.success("获取成功", flowDispatchService.checkDueDispatches());
    }

    /** 自动下发所有到期待下发的期次 */
    @PostMapping("/auto-dispatch")
    public Result<Integer> autoDispatch() {
        try {
            return Result.success("下发成功", flowDispatchService.autoDispatchDuePeriods());
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 创建任务（含下发周期配置、模板配置信息、人员） */
    @PostMapping("/save")
    public Result<String> save(@RequestBody TaskSaveDTO dto) {
        try {
            return Result.success("创建成功", flowDispatchService.save(dto));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 更新任务 */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody TaskSaveDTO dto) {
        try {
            return flowDispatchService.update(dto) ? Result.success("更新成功") : Result.fail("更新失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable String id) {
        try {
            return flowDispatchService.toggleStatus(id) ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 设置/取消样例：仅超管可操作（样例公共可见、不可改） */
    @PutMapping("/sample/{id}")
    public Result<Void> toggleSample(@PathVariable String id) {
        try {
            return flowDispatchService.toggleSample(id) ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 删除任务：仅当任务下无任何期次时允许 */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            return flowDispatchService.delete(id) ? Result.success("删除成功") : Result.fail("删除失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 任务详情（编辑回填用） */
    @GetMapping("/{id}")
    public Result<FlowDispatch> detail(@PathVariable String id) {
        try {
            return Result.success("获取成功", flowDispatchService.getById(id));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 任务人员列表 */
    @GetMapping("/{taskId}/members")
    public Result<List<TaskMemberInfoVO>> members(@PathVariable String taskId) {
        return Result.success("获取成功", flowDispatchService.getMembers(taskId));
    }

    /** 期次新增人员：不重新生成期次，抄用期次配置为每位新人员创建独立提交任务 */
    @PostMapping("/{dispatchId}/period/add-members")
    public Result<Integer> periodAddMembers(@PathVariable String dispatchId, @RequestBody Map<String, List<String>> body) {
        try {
            int count = flowDispatchService.addMembersToDispatch(dispatchId, body == null ? null : body.get("userIds"));
            return Result.success("新增成功", count);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 全量保存任务人员（后续生成期次抄用） */
    @PutMapping("/{taskId}/members")
    public Result<Void> saveMembers(@PathVariable String taskId, @RequestBody List<String> userIds) {
        flowDispatchService.saveMembers(taskId, userIds);
        return Result.success("人员已更新");
    }

    /**
     * 生成期次：自动（按任务周期 + immediate 立即/下一期次）或手动临时期次（manual=true）
     * body: { immediate, periodName, manual, memberIds, startTime, endTime }
     * memberIds 为本期次临时人员（可对任务人员临时增删，为空则抄用任务配置人员）
     * startTime/endTime 为临时期次自定义起止时间（manual=true 时生效，格式 yyyy-MM-dd HH:mm:ss）
     * 自动下发防重复：当前期次（如 2026年第3季度）已下发时拒绝再次下发。
     * 返回：期次ID + 期次名称 + 下次自动下发时间。
     */
    @PostMapping("/{taskId}/periods")
    public Result<PeriodGenerateVO> generatePeriod(@PathVariable String taskId, @RequestBody(required = false) Map<String, Object> body) {
        try {
            boolean manual = body != null && Boolean.TRUE.equals(body.get("manual"));
            boolean immediate = body == null || !Boolean.FALSE.equals(body.get("immediate"));
            String periodName = body == null ? null : (String) body.get("periodName");
            List<String> memberIds = null;
            if (body != null && body.get("memberIds") instanceof List) {
                memberIds = ((List<?>) body.get("memberIds")).stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .collect(Collectors.toList());
            }
            // 本期次人员任务名临时覆盖（userId → 任务名），仅本期生效
            Map<String, String> memberTaskNames = null;
            if (body != null && body.get("memberTaskNames") instanceof Map) {
                memberTaskNames = new java.util.LinkedHashMap<>();
                for (Map.Entry<?, ?> en : ((Map<?, ?>) body.get("memberTaskNames")).entrySet()) {
                    if (en.getKey() != null && en.getValue() != null) {
                        memberTaskNames.put(String.valueOf(en.getKey()), String.valueOf(en.getValue()));
                    }
                }
            }
            Date startTime = parseDate(body == null ? null : body.get("startTime"));
            Date endTime = parseDate(body == null ? null : body.get("endTime"));
            // 是否下发后通知各处理人（手动下发弹窗勾选；未传默认通知）
            boolean notifyMembers = body == null || !Boolean.FALSE.equals(body.get("notify"));
            PeriodGenerateVO vo = flowDispatchService.generatePeriod(taskId, immediate, periodName, manual, memberIds, startTime, endTime, memberTaskNames, notifyMembers);
            return Result.success("期次生成成功", vo);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 修改期次截止时间：同步更新该期次下所有成员任务的截止时间。
     * body: { endTime: "yyyy-MM-dd HH:mm:ss" }
     */
    @PutMapping("/period/{id}/end-time")
    public Result<Void> updatePeriodEndTime(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            flowDispatchService.updatePeriodEndTime(id, parseDate(body == null ? null : body.get("endTime")));
            return Result.success("截止时间已更新");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 期次详情（含截止时间、催办时间），期次人员页展示用 */
    @GetMapping("/period/{id}")
    public Result<Map<String, Object>> periodInfo(@PathVariable String id) {
        try {
            return Result.success("获取成功", flowDispatchService.getPeriodInfo(id));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 解析前端传来的日期时间字符串（yyyy-MM-dd HH:mm:ss），非字符串/空值返回 null */
    private Date parseDate(Object value) {
        if (!(value instanceof String)) return null;
        String s = ((String) value).trim();
        if (s.isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException("日期时间格式不正确：" + s);
        }
    }
}

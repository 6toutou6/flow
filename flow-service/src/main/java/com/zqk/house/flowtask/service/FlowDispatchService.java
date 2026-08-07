package com.zqk.house.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqk.house.flowtask.entity.FlowDispatch;
import com.zqk.house.flowtask.entity.FlowDispatchConfig;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.entity.FlowTaskDispatch;
import com.zqk.house.flowtask.entity.FlowTaskMember;
import com.zqk.house.flowtask.entity.FlowTaskNode;
import com.zqk.house.flowtask.mapper.FlowDispatchConfigMapper;
import com.zqk.house.flowtask.mapper.FlowDispatchMapper;
import com.zqk.house.flowtask.mapper.FlowTaskDispatchMapper;
import com.zqk.house.flowtask.mapper.FlowTaskMapper;
import com.zqk.house.flowtask.mapper.FlowTaskMemberMapper;
import com.zqk.house.flowtask.mapper.FlowTaskNodeMapper;
import com.zqk.house.flowtask.vo.PeriodGenerateVO;
import com.zqk.house.flowtask.vo.PeriodPreviewVO;
import com.zqk.house.flowtask.vo.TaskMemberInfoVO;
import com.zqk.house.flowtask.vo.TaskSaveDTO;
import com.zqk.house.flowtemplate.entity.FlowTemplate;
import com.zqk.house.flowtemplate.entity.FlowTemplateNode;
import com.zqk.house.flowtemplate.mapper.FlowTemplateMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateNodeMapper;
import com.zqk.house.sysuser.entity.SysUser;
import com.zqk.house.sysuser.mapper.SysUserMapper;
import com.zqk.house.sysuser.vo.LoginUser;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务服务（flow_dispatch）：任务 → 期次 → 人员
 * 下发周期配置独立存放于 flow_dispatch_config（每任务一份），生成期次时抄用计算开始/截止时间。
 * 周期期次：截止日期 = 触发日 + deadline_days（补发且已过期则顺延为当前 + deadline_days）。
 * 自动下发防重复：同一任务下同一期间（period_key，如 2026-Q3）不允许重复下发。
 * 生成期次后返回下次自动下发时间（下一个未下发的周期开始时间）。
 */
@Service
public class FlowDispatchService {

    /** 周期类型：每周 */
    public static final int CYCLE_WEEK = 1;
    /** 周期类型：每月 */
    public static final int CYCLE_MONTH = 2;
    /** 周期类型：每季度 */
    public static final int CYCLE_QUARTER = 3;
    /** 周期类型：单次下发 */
    public static final int CYCLE_ONCE = 4;

    @Autowired
    private FlowDispatchMapper flowDispatchMapper;
    @Autowired
    private FlowDispatchConfigMapper flowDispatchConfigMapper;
    @Autowired
    private FlowTaskMemberMapper flowTaskMemberMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTemplateMapper flowTemplateMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private FlowTemplateNodeMapper flowTemplateNodeMapper;
    @Autowired
    private ObjectMapper objectMapper;

    /** 模板级字段值 Map → JSON（null/空返回 null） */
    private String serializeTemplateData(Map<Long, String> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON → 模板级字段值 Map（null/空返回空 Map） */
    private Map<Long, String> deserializeTemplateData(String json) {
        if (!StringUtils.hasText(json)) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<Long, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // ==================== 下发配置 ====================

    /** 读取任务下发配置（每任务一份；无配置返回 null） */
    private FlowDispatchConfig getConfig(Long taskId) {
        if (taskId == null) return null;
        return flowDispatchConfigMapper.selectOne(new LambdaQueryWrapper<FlowDispatchConfig>()
                .eq(FlowDispatchConfig::getTaskId, taskId));
    }

    /** 填充任务的下发配置字段（编辑回填用；无配置按单次下发兜底） */
    private FlowDispatch fillConfig(FlowDispatch task) {
        if (task == null) return null;
        FlowDispatchConfig cfg = getConfig(task.getId());
        if (cfg != null) {
            task.setCycleType(cfg.getCycleType());
            task.setCycleDay(cfg.getCycleDay());
            task.setDeadlineDays(cfg.getDeadlineDays());
            task.setUrgeDays(cfg.getUrgeDays());
        } else {
            task.setCycleType(CYCLE_ONCE);
        }
        return task;
    }

    /** 保存下发配置（每任务一份，存在则更新） */
    private void saveConfig(Long taskId, Integer cycleType, Integer cycleDay, Integer deadlineDays, Integer urgeDays) {
        if (taskId == null) return;
        FlowDispatchConfig cfg = getConfig(taskId);
        if (cfg == null) cfg = new FlowDispatchConfig();
        cfg.setTaskId(taskId);
        cfg.setCycleType(cycleType == null ? CYCLE_ONCE : cycleType);
        cfg.setCycleDay(cycleDay);
        cfg.setDeadlineDays(deadlineDays);
        cfg.setUrgeDays(urgeDays);
        cfg.setUpdateTime(new Date());
        if (cfg.getId() == null) {
            cfg.setCreateTime(new Date());
            flowDispatchConfigMapper.insert(cfg);
        } else {
            flowDispatchConfigMapper.updateById(cfg);
        }
    }

    // ==================== 任务 CRUD ====================

    // ---- 可见性与权限 ----

    private boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
    }

    /** 当前用户可见性过滤的 deptId：超管返回 null（全量），普通用户返回其部门ID */
    private Long visibleDeptId(LoginUser loginUser) {
        return isSuperAdmin(loginUser) ? null : (loginUser == null ? null : loginUser.getDeptId());
    }

    /** 操作权限校验：超管全量；样例仅超管可改；普通任务需同部门 */
    private void checkPermission(FlowDispatch d) {
        if (d == null) throw new RuntimeException("任务不存在");
        if (isSuperAdmin(SecurityUtils.getLoginUser())) return;
        if (d.getIsSample() != null && d.getIsSample() == 1) {
            throw new RuntimeException("样例任务仅超管可修改");
        }
        if (d.getDeptId() != null) {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null || !Objects.equals(d.getDeptId(), loginUser.getDeptId())) {
                throw new RuntimeException("无权操作其他部门的任务");
            }
        }
    }

    /** 任务分页（含下发配置、期次数、人员数） */
    public PageResult<FlowDispatch> getPage(Integer page, Integer limit, String taskName, Integer status) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        String n = StringUtils.hasText(taskName) ? taskName.trim() : null;
        Long deptId = visibleDeptId(SecurityUtils.getLoginUser());
        List<FlowDispatch> list = flowDispatchMapper.selectTaskPage(n, status, deptId, offset, l);
        Long total = flowDispatchMapper.selectTaskCount(n, status, deptId);
        fillCreatorInfo(list);
        return new PageResult<>(list, total);
    }

    /** 批量补充创建人姓名/部门（按 creatorId 查 sys_user） */
    private void fillCreatorInfo(List<FlowDispatch> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(FlowDispatch::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        for (FlowDispatch d : list) {
            SysUser u = d.getCreatorId() == null ? null : userMap.get(d.getCreatorId());
            if (u != null) {
                d.setCreatorName(u.getRealName());
                d.setDeptName(u.getDeptName());
            }
        }
    }

    /** 任务详情（编辑回填用，含下发配置） */
    public FlowDispatch getById(Long id) {
        if (id == null) return null;
        return fillConfig(flowDispatchMapper.selectById(id));
    }

    /** 启用中的任务列表（部门可见性过滤） */
    public List<FlowDispatch> getEnabledList() {
        LambdaQueryWrapper<FlowDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDispatch::getStatus, 1);
        Long deptId = visibleDeptId(SecurityUtils.getLoginUser());
        if (deptId != null) {
            wrapper.and(w -> w.eq(FlowDispatch::getIsSample, 1).or().eq(FlowDispatch::getDeptId, deptId));
        }
        wrapper.orderByDesc(FlowDispatch::getCreateTime);
        return flowDispatchMapper.selectList(wrapper);
    }

    /** 创建任务（下发周期配置写入 flow_dispatch_config） */
    @Transactional(rollbackFor = Exception.class)
    public Long save(TaskSaveDTO dto) {
        validate(dto);
        FlowDispatch task = new FlowDispatch();
        task.setTemplateId(dto.getTemplateId());
        task.setTaskName(dto.getTaskName().trim());
        task.setTaskDesc(dto.getTaskDesc());
        task.setTemplateData(serializeTemplateData(dto.getTemplateData()));
        task.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        LoginUser loginUser = SecurityUtils.getLoginUser();
        task.setCreatorId(loginUser == null ? null : loginUser.getId());
        task.setDeptId(loginUser == null ? null : loginUser.getDeptId());
        // 样例仅超管通过单独接口设置，普通创建一律为普通任务
        task.setIsSample(0);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        flowDispatchMapper.insert(task);
        saveConfig(task.getId(), dto.getCycleType(), dto.getCycleDay(), dto.getDeadlineDays(), dto.getUrgeDays());
        saveMembers(task.getId(), dto.getMemberIds());
        return task.getId();
    }

    /** 更新任务（下发周期配置同步更新 flow_dispatch_config） */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(TaskSaveDTO dto) {
        if (dto.getId() == null) throw new RuntimeException("参数缺失");
        FlowDispatch task = flowDispatchMapper.selectById(dto.getId());
        if (task == null) throw new RuntimeException("任务不存在");
        checkPermission(task);
        validate(dto);
        task.setTemplateId(dto.getTemplateId());
        task.setTaskName(dto.getTaskName().trim());
        task.setTaskDesc(dto.getTaskDesc());
        if (dto.getTemplateData() != null) {
            task.setTemplateData(serializeTemplateData(dto.getTemplateData()));
        }
        task.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        task.setUpdateTime(new Date());
        flowDispatchMapper.updateById(task);
        saveConfig(dto.getId(), dto.getCycleType(), dto.getCycleDay(), dto.getDeadlineDays(), dto.getUrgeDays());
        if (dto.getMemberIds() != null) {
            saveMembers(dto.getId(), dto.getMemberIds());
        }
        return true;
    }

    /** 设置/取消样例：仅超管可操作（样例公共可见、普通用户不可改） */
    public boolean toggleSample(Long id) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!isSuperAdmin(loginUser)) throw new RuntimeException("仅超管可设置样例");
        FlowDispatch d = flowDispatchMapper.selectById(id);
        if (d == null) return false;
        d.setIsSample(d.getIsSample() == null || d.getIsSample() == 0 ? 1 : 0);
        d.setUpdateTime(new Date());
        return flowDispatchMapper.updateById(d) > 0;
    }

    private void validate(TaskSaveDTO dto) {
        if (!StringUtils.hasText(dto.getTaskName())) throw new RuntimeException("请填写任务名称");
        if (dto.getTemplateId() == null) throw new RuntimeException("请选择流程模板");
        Integer cycle = dto.getCycleType() == null ? CYCLE_ONCE : dto.getCycleType();
        if (cycle != CYCLE_ONCE && dto.getCycleDay() == null) throw new RuntimeException("请选择触发日");
    }

    public boolean toggleStatus(Long id) {
        FlowDispatch d = flowDispatchMapper.selectById(id);
        if (d == null) return false;
        checkPermission(d);
        d.setStatus(d.getStatus() == null || d.getStatus() == 0 ? 1 : 0);
        d.setUpdateTime(new Date());
        return flowDispatchMapper.updateById(d) > 0;
    }

    /** 删除任务：仅当任务下无任何期次时允许（同时删除下发配置） */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        checkPermission(flowDispatchMapper.selectById(id));
        Long periodCount = flowTaskDispatchMapper.selectCount(new LambdaQueryWrapper<FlowTaskDispatch>()
                .eq(FlowTaskDispatch::getTaskId, id));
        if (periodCount != null && periodCount > 0) {
            throw new RuntimeException("请先删除该任务下的所有期次");
        }
        flowTaskMemberMapper.delete(new LambdaQueryWrapper<FlowTaskMember>().eq(FlowTaskMember::getTaskId, id));
        flowDispatchConfigMapper.delete(new LambdaQueryWrapper<FlowDispatchConfig>().eq(FlowDispatchConfig::getTaskId, id));
        return flowDispatchMapper.deleteById(id) > 0;
    }

    // ==================== 任务人员配置 ====================

    /** 任务人员列表 */
    public List<TaskMemberInfoVO> getMembers(Long taskId) {
        return flowDispatchMapper.selectMembersByTask(taskId);
    }

    /** 全量保存任务人员（后续生成期次抄用） */
    @Transactional(rollbackFor = Exception.class)
    public void saveMembers(Long taskId, List<Long> userIds) {
        if (taskId == null) return;
        flowTaskMemberMapper.delete(new LambdaQueryWrapper<FlowTaskMember>().eq(FlowTaskMember::getTaskId, taskId));
        if (userIds == null) return;
        for (Long uid : userIds) {
            if (uid == null) continue;
            FlowTaskMember m = new FlowTaskMember();
            m.setTaskId(taskId);
            m.setUserId(uid);
            try {
                flowTaskMemberMapper.insert(m);
            } catch (Exception ignored) {
                // 唯一键冲突（重复人员）跳过
            }
        }
    }

    // ==================== 周期计算（周/月/季） ====================

    /**
     * 计算某个周期窗口（当前或下一周期）。
     * @param current true=from 所在的当期；false=from 之后的下一周期
     */
    private PeriodWindow nextWindow(int cycle, Integer cycleDay, LocalDate from, boolean current) {
        PeriodWindow pw = new PeriodWindow();
        int day = cycleDay == null ? 1 : Math.max(1, cycleDay);
        if (cycle == CYCLE_WEEK) {
            int iso = from.getDayOfWeek().getValue(); // Mon=1..Sun=7
            LocalDate monday = from.minusDays(iso - 1);
            if (!current) monday = monday.plusWeeks(1);
            LocalDate start = monday.plusDays(Math.min(day, 7) - 1);
            int year = start.get(WeekFields.ISO.weekBasedYear());
            int weekNo = start.get(WeekFields.ISO.weekOfWeekBasedYear());
            pw.start = start;
            pw.key = year + "-W" + String.format("%02d", weekNo);
            pw.name = year + "年第" + weekNo + "周";
        } else if (cycle == CYCLE_MONTH) {
            LocalDate first = from.withDayOfMonth(1);
            if (!current) first = first.plusMonths(1);
            LocalDate start = first.withDayOfMonth(Math.min(day, first.lengthOfMonth()));
            String ym = first.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            pw.start = start;
            pw.key = ym;
            pw.name = ym;
        } else { // CYCLE_QUARTER
            int curQ = (from.getMonthValue() - 1) / 3 + 1;
            int q = current ? curQ : (curQ == 4 ? 1 : curQ + 1);
            int month = (q - 1) * 3 + 1;
            int year = from.getYear();
            if (q < curQ) year = from.getYear() + 1; // 跨年到下一季度
            LocalDate first = LocalDate.of(year, month, 1);
            LocalDate start = first.withDayOfMonth(Math.min(day, first.lengthOfMonth()));
            pw.start = start;
            pw.key = year + "-Q" + q;
            pw.name = year + "年第" + q + "季度";
        }
        return pw;
    }

    /** 指定基准之后第一个「未下发」周期窗口（自动跳过已下发的期次，防重复；firstCurrent 控制是否先取基准当期） */
    private PeriodWindow nextUndispatchedWindow(int cycle, Integer cycleDay, LocalDate from, boolean firstCurrent, Long taskId) {
        PeriodWindow pw = nextWindow(cycle, cycleDay, from, firstCurrent);
        int guard = 0;
        while (isDispatched(taskId, pw.key, pw.name) && guard++ < 500) {
            pw = nextWindow(cycle, cycleDay, pw.start, true);
        }
        return pw;
    }

    /**
     * 当前期次是否已下发：优先按期间标识 period_key 检测；period_key 为空（旧数据/手动期次）时按期次名兜底匹配。
     */
    private boolean isDispatched(Long taskId, String periodKey, String periodName) {
        if (taskId == null) return false;
        if (StringUtils.hasText(periodKey)) {
            Long cnt = flowTaskDispatchMapper.selectCount(new LambdaQueryWrapper<FlowTaskDispatch>()
                    .eq(FlowTaskDispatch::getTaskId, taskId)
                    .eq(FlowTaskDispatch::getPeriodKey, periodKey));
            if (cnt != null && cnt > 0) return true;
        }
        if (StringUtils.hasText(periodName)) {
            Long cnt2 = flowTaskDispatchMapper.selectCount(new LambdaQueryWrapper<FlowTaskDispatch>()
                    .eq(FlowTaskDispatch::getTaskId, taskId)
                    .isNull(FlowTaskDispatch::getPeriodKey)
                    .eq(FlowTaskDispatch::getPeriodName, periodName));
            if (cnt2 != null && cnt2 > 0) return true;
        }
        return false;
    }

    /** 按触发日 + 截止天数计算截止时间；补发且已过期则从今天起给足天数 */
    private Date calcEnd(LocalDate start, Integer deadlineDays) {
        Date end = addDays(toDate(start), deadlineDays);
        if (deadlineDays != null && end.before(new Date())) {
            end = addDays(new Date(), deadlineDays);
        }
        return end;
    }

    // ==================== 期次预览 ====================

    /**
     * 期次预览：根据任务下发配置周期 + 是否立即下发，计算期次序号、默认期次名、开始/截止时间、
     * 期间标识（periodKey）、当前期次是否已下发、下次自动下发时间。
     * 立即下发 → 当期次（已下发则标记 alreadyDispatched，禁止重复）；非立即 → 下一个未下发期次。
     */
    public PeriodPreviewVO previewPeriod(Long taskId, boolean immediate) {
        PeriodPreviewVO vo = new PeriodPreviewVO();
        FlowDispatch task = taskId == null ? null : flowDispatchMapper.selectById(taskId);
        if (task == null) {
            vo.setStartTime(new Date());
            return vo;
        }
        FlowDispatchConfig cfg = getConfig(taskId);
        if (cfg == null) cfg = new FlowDispatchConfig();
        int cycle = cfg.getCycleType() == null ? CYCLE_ONCE : cfg.getCycleType();
        Integer day = cfg.getCycleDay();
        vo.setPeriodNo(nextPeriodNo(taskId));
        vo.setUrgeDays(cfg.getUrgeDays());
        LocalDate now = LocalDate.now();
        if (cycle == CYCLE_ONCE) {
            // 单次下发：期次名由用户填，截止 = 下发日 + deadlineDays
            vo.setPeriodName(null);
            vo.setStartTime(new Date());
            vo.setEndTime(addDays(new Date(), cfg.getDeadlineDays()));
            return vo;
        }
        if (immediate) {
            // 立即下发：当期窗口；已下发则提示，禁止重复
            // 截止时间按下发时间计算（补发当期时避免按触发日+天数导致立即截止）
            PeriodWindow cur = nextWindow(cycle, day, now, true);
            vo.setPeriodKey(cur.key);
            vo.setPeriodName(cur.name);
            vo.setStartTime(new Date());
            vo.setEndTime(calcEnd(now, cfg.getDeadlineDays()));
            vo.setAlreadyDispatched(isDispatched(taskId, cur.key, cur.name));
            // 下次下发：本期之后的下一个未下发周期
            PeriodWindow next = nextUndispatchedWindow(cycle, day, cur.start, false, taskId);
            vo.setNextDispatchTime(toDate(next.start));
        } else {
            // 下一期次下发：自动跳过已下发的期次
            PeriodWindow next = nextUndispatchedWindow(cycle, day, now, false, taskId);
            vo.setPeriodKey(next.key);
            vo.setPeriodName(next.name);
            vo.setStartTime(toDate(next.start));
            vo.setEndTime(calcEnd(next.start, cfg.getDeadlineDays()));
            vo.setAlreadyDispatched(false);
            // 下次下发：本期之后的下一个未下发周期
            PeriodWindow after = nextUndispatchedWindow(cycle, day, next.start, false, taskId);
            vo.setNextDispatchTime(toDate(after.start));
        }
        return vo;
    }

    /** 同一任务下期次序号：已建期次最大序号 + 1 */
    public Integer nextPeriodNo(Long taskId) {
        if (taskId == null) return null;
        LambdaQueryWrapper<FlowTaskDispatch> pw = new LambdaQueryWrapper<>();
        pw.eq(FlowTaskDispatch::getTaskId, taskId)
          .orderByDesc(FlowTaskDispatch::getPeriodNo)
          .last("LIMIT 1");
        FlowTaskDispatch last = flowTaskDispatchMapper.selectOne(pw);
        return (last == null || last.getPeriodNo() == null) ? 1 : last.getPeriodNo() + 1;
    }

    // ==================== 期次生成 ====================

    /**
     * 生成期次：自动（按任务下发配置周期 + 是否立即下发）或手动临时期次。
     * 自动下发防重复：当期次已下发（period_key 相同）时禁止再次下发。
     * 期次抄用任务配置：模板、模板配置信息（template_data）、任务名/说明、起止时间、人员（每人一条独立提交任务）。
     * @param memberIds 本期次人员ID（临时指定，可对任务人员做临时增删；为空则抄用任务配置人员）
     * @param manual true=手动临时期次（期次名必填/默认第N期，不占用自动周期编排逻辑）
     * @return 生成结果（期次ID、期次名、下次自动下发时间）
     */
    @Transactional(rollbackFor = Exception.class)
    public PeriodGenerateVO generatePeriod(Long taskId, boolean immediate, String periodName, boolean manual, List<Long> memberIds) {
        FlowDispatch task = flowDispatchMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        if (task.getStatus() != null && task.getStatus() == 0) {
            throw new RuntimeException("任务已停用，请先启用后再生成期次");
        }
        FlowTemplate tpl = flowTemplateMapper.selectById(task.getTemplateId());
        if (tpl == null) throw new RuntimeException("模板不存在");
        // 开始节点
        LambdaQueryWrapper<FlowTemplateNode> sw = new LambdaQueryWrapper<>();
        sw.eq(FlowTemplateNode::getTemplateId, task.getTemplateId())
          .eq(FlowTemplateNode::getNodeType, 1).last("LIMIT 1");
        FlowTemplateNode firstNode = flowTemplateNodeMapper.selectOne(sw);
        if (firstNode == null) throw new RuntimeException("模板未设计流程节点（缺少开始节点）");
        // 本期次人员：优先用调用方临时指定的人员（临时增删不影响任务配置），否则抄用任务配置人员
        List<Long> memberUids;
        if (memberIds != null && !memberIds.isEmpty()) {
            memberUids = memberIds.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            List<FlowTaskMember> members = flowTaskMemberMapper.selectList(
                    new LambdaQueryWrapper<FlowTaskMember>().eq(FlowTaskMember::getTaskId, taskId));
            if (members.isEmpty()) throw new RuntimeException("请先在任务中配置人员");
            memberUids = members.stream().map(FlowTaskMember::getUserId).filter(Objects::nonNull).collect(Collectors.toList());
        }
        if (memberUids.isEmpty()) throw new RuntimeException("请选择本期次人员");
        // 总节点数
        long total = flowTemplateNodeMapper.selectCount(
                new LambdaQueryWrapper<FlowTemplateNode>().eq(FlowTemplateNode::getTemplateId, task.getTemplateId()));
        LoginUser loginUser = SecurityUtils.getLoginUser();
        FlowDispatchConfig cfg = getConfig(taskId);
        if (cfg == null) cfg = new FlowDispatchConfig();
        int cycle = cfg.getCycleType() == null ? CYCLE_ONCE : cfg.getCycleType();
        Integer day = cfg.getCycleDay();
        // 期次信息：自动按周期预览；手动临时期次由用户命名
        Integer periodNo = nextPeriodNo(taskId);
        String periodNameFinal;
        Date startTime;
        Date endTime;
        String periodKey = null;
        Date nextDispatchTime = null;
        if (manual) {
            periodNameFinal = StringUtils.hasText(periodName) ? periodName.trim() : "第" + periodNo + "期";
            startTime = new Date();
            endTime = addDays(new Date(), cfg.getDeadlineDays());
        } else {
            PeriodPreviewVO preview = previewPeriod(taskId, immediate);
            if (Boolean.TRUE.equals(preview.getAlreadyDispatched())) {
                throw new RuntimeException("当前期次「" + preview.getPeriodName() + "」已下发，请勿重复下发");
            }
            periodNameFinal = StringUtils.hasText(periodName) ? periodName.trim() : preview.getPeriodName();
            startTime = preview.getStartTime();
            endTime = preview.getEndTime();
            periodKey = preview.getPeriodKey();
            nextDispatchTime = preview.getNextDispatchTime();
        }
        if (!StringUtils.hasText(periodNameFinal)) periodNameFinal = "第" + periodNo + "期";
        // 手动临时期次也算出下次自动下发时间（非单次周期任务）
        if (cycle != CYCLE_ONCE && nextDispatchTime == null) {
            LocalDate base = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            PeriodWindow after = nextUndispatchedWindow(cycle, day, base, false, taskId);
            nextDispatchTime = toDate(after.start);
        }
        // 建期次（抄用任务配置）
        FlowTaskDispatch dispatch = new FlowTaskDispatch();
        dispatch.setTaskId(taskId);
        dispatch.setTemplateId(task.getTemplateId());
        dispatch.setTemplateVersion(tpl.getVersion());
        dispatch.setPeriodNo(periodNo);
        dispatch.setPeriodName(periodNameFinal);
        dispatch.setPeriodKey(periodKey);
        dispatch.setManualFlag(manual ? 1 : 0);
        dispatch.setTaskName(task.getTaskName());
        dispatch.setTaskDesc(task.getTaskDesc());
        dispatch.setTemplateData(task.getTemplateData());
        dispatch.setStartTime(startTime);
        dispatch.setEndTime(endTime);
        dispatch.setCreatorId(loginUser == null ? null : loginUser.getId());
        flowTaskDispatchMapper.insert(dispatch);
        // 为每位人员创建独立提交任务（从开始节点重新走流程）
        for (Long uid : memberUids) {
            FlowTask ft = new FlowTask();
            ft.setTemplateId(task.getTemplateId());
            ft.setTaskName(task.getTaskName());
            ft.setTaskDesc(task.getTaskDesc());
            ft.setTemplateData(task.getTemplateData());
            ft.setStartTime(startTime);
            ft.setEndTime(endTime);
            ft.setStatus(1);
            ft.setTemplateVersion(tpl.getVersion());
            ft.setCurrentNodeId(firstNode.getId());
            ft.setCurrentHandlerId(uid);
            ft.setFinishedNodeCount(0);
            ft.setTotalNodeCount((int) total);
            ft.setCreatorId(dispatch.getCreatorId());
            ft.setDispatchId(dispatch.getId());
            flowTaskMapper.insert(ft);
            FlowTaskNode n = new FlowTaskNode();
            n.setTaskId(ft.getId());
            n.setNodeId(firstNode.getId());
            n.setNodeName(firstNode.getNodeName());
            n.setSortNum(firstNode.getSortNum());
            n.setNodeType(firstNode.getNodeType());
            n.setHandlerUserId(uid);
            n.setSubmitStatus(0);
            n.setAction(0);
            flowTaskNodeMapper.insert(n);
        }
        PeriodGenerateVO vo = new PeriodGenerateVO();
        vo.setPeriodId(dispatch.getId());
        vo.setPeriodName(periodNameFinal);
        vo.setNextDispatchTime(nextDispatchTime);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public int addMembersToDispatch(Long dispatchId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) throw new RuntimeException("请选择要新增的人员");
        List<Long> uids = userIds.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
        if (uids.isEmpty()) throw new RuntimeException("请选择要新增的人员");
        FlowTaskDispatch dispatch = flowTaskDispatchMapper.selectById(dispatchId);
        if (dispatch == null) throw new RuntimeException("期次不存在");
        FlowTemplate tpl = flowTemplateMapper.selectById(dispatch.getTemplateId());
        if (tpl == null) throw new RuntimeException("模板不存在");
        // 开始节点
        LambdaQueryWrapper<FlowTemplateNode> sw = new LambdaQueryWrapper<>();
        sw.eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId())
          .eq(FlowTemplateNode::getNodeType, 1).last("LIMIT 1");
        FlowTemplateNode firstNode = flowTemplateNodeMapper.selectOne(sw);
        if (firstNode == null) throw new RuntimeException("模板未设计流程节点（缺少开始节点）");
        long total = flowTemplateNodeMapper.selectCount(
                new LambdaQueryWrapper<FlowTemplateNode>().eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId()));
        // 该期次已存在的人员（从节点处理人判定，避免重复新增）
        List<Long> taskIds = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>().eq(FlowTask::getDispatchId, dispatchId))
                .stream().map(FlowTask::getId).collect(Collectors.toList());
        List<Long> existing = new ArrayList<>();
        if (!taskIds.isEmpty()) {
            for (FlowTaskNode n : flowTaskNodeMapper.selectList(new LambdaQueryWrapper<FlowTaskNode>()
                    .in(FlowTaskNode::getTaskId, taskIds).select(FlowTaskNode::getHandlerUserId))) {
                if (n.getHandlerUserId() != null && !existing.contains(n.getHandlerUserId())) {
                    existing.add(n.getHandlerUserId());
                }
            }
        }
        // 抄用期次配置为每位新人员创建独立提交任务（从开始节点重新走流程）
        int count = 0;
        for (Long uid : uids) {
            if (existing.contains(uid)) continue;
            FlowTask ft = new FlowTask();
            ft.setTemplateId(dispatch.getTemplateId());
            ft.setDispatchId(dispatchId);
            ft.setTaskName(dispatch.getTaskName());
            ft.setTaskDesc(dispatch.getTaskDesc());
            ft.setTemplateData(dispatch.getTemplateData());
            ft.setStartTime(dispatch.getStartTime());
            ft.setEndTime(dispatch.getEndTime());
            ft.setStatus(1);
            ft.setTemplateVersion(dispatch.getTemplateVersion());
            ft.setCurrentNodeId(firstNode.getId());
            ft.setCurrentHandlerId(uid);
            ft.setFinishedNodeCount(0);
            ft.setTotalNodeCount((int) total);
            ft.setCreatorId(dispatch.getCreatorId());
            flowTaskMapper.insert(ft);
            FlowTaskNode n = new FlowTaskNode();
            n.setTaskId(ft.getId());
            n.setNodeId(firstNode.getId());
            n.setNodeName(firstNode.getNodeName());
            n.setSortNum(firstNode.getSortNum());
            n.setNodeType(firstNode.getNodeType());
            n.setHandlerUserId(uid);
            n.setSubmitStatus(0);
            n.setAction(0);
            flowTaskNodeMapper.insert(n);
            count++;
        }
        if (count == 0) throw new RuntimeException("所选人员均已在该期次中，无需重复新增");
        return count;
    }

    private static class PeriodWindow {
        LocalDate start;
        String key;
        String name;
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date addDays(Date d, Integer days) {
        if (d == null) return null;
        int n = days == null ? 0 : days;
        LocalDateTime ldt = LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        return Date.from(ldt.plusDays(n).atZone(ZoneId.systemDefault()).toInstant());
    }
}

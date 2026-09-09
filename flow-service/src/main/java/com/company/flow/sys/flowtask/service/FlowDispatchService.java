package com.company.flow.sys.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.flow.sys.base.attach.entity.Attach;
import com.company.flow.sys.base.attach.service.AttachService;
import com.company.flow.sys.base.enums.CycleType;
import com.company.flow.sys.base.enums.NodeType;
import com.company.flow.sys.flowtask.entity.FlowDispatch;
import com.company.flow.sys.flowtask.entity.FlowDispatchConfig;
import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.base.robot.service.RobotService;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatchNode;
import com.company.flow.sys.flowtask.entity.FlowTaskMember;
import com.company.flow.sys.flowtask.entity.FlowTaskNode;
import com.company.flow.sys.flowtask.mapper.FlowDispatchConfigMapper;
import com.company.flow.sys.flowtask.mapper.FlowDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchNodeMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMemberMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskNodeMapper;
import com.company.flow.sys.flowtask.vo.DispatchStatsVO;
import com.company.flow.sys.flowtask.vo.DueDispatchVO;
import com.company.flow.sys.flowtask.vo.PeriodGenerateVO;
import com.company.flow.sys.flowtask.vo.PeriodPreviewVO;
import com.company.flow.sys.flowtask.vo.TaskMemberInfoVO;
import com.company.flow.sys.flowtask.vo.TaskSaveDTO;
import com.company.flow.sys.flowtemplate.entity.FlowTemplate;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateField;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateNode;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateMapper;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateNodeMapper;
import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.mapper.UserMapper;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
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

    private static final Logger log = LoggerFactory.getLogger(FlowDispatchService.class);

    @Autowired
    private FlowDispatchMapper flowDispatchMapper;
    @Autowired
    private FlowDispatchConfigMapper flowDispatchConfigMapper;
    @Autowired
    private FlowTaskMemberMapper flowTaskMemberMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;

    @Autowired
    private RobotService robotService;
    @Autowired
    private FlowNotifyService flowNotifyService;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTaskDispatchNodeMapper flowTaskDispatchNodeMapper;
    @Autowired
    private FlowTemplateMapper flowTemplateMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private com.company.flow.sys.base.deptAdmin.service.DeptAdminService deptAdminService;
    @Autowired
    private FlowTemplateNodeMapper flowTemplateNodeMapper;
    @Autowired
    private FlowTemplateFieldMapper flowTemplateFieldMapper;
    @Autowired
    private AttachService attachService;
    @Autowired
    private ObjectMapper objectMapper;

    /** 模板级字段值 Map → JSON（null/空返回 null） */
    private String serializeTemplateData(Map<String, String> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON → 模板级字段值 Map（null/空返回空 Map） */
    private Map<String, String> deserializeTemplateData(String json) {
        if (!StringUtils.hasText(json)) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // ==================== 下发配置 ====================

    /** 读取任务下发配置（每任务一份；无配置返回 null） */
    private FlowDispatchConfig getConfig(String taskId) {
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
            task.setCycleType(CycleType.ONCE.getCode());
        }
        return task;
    }

    /** 保存下发配置（每任务一份，存在则更新） */
    private void saveConfig(String taskId, Integer cycleType, Integer cycleDay, Integer deadlineDays, Integer urgeDays) {
        if (taskId == null) return;
        FlowDispatchConfig cfg = getConfig(taskId);
        if (cfg == null) cfg = new FlowDispatchConfig();
        cfg.setTaskId(taskId);
        cfg.setCycleType(cycleType == null ? CycleType.ONCE.getCode() : cycleType);
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

    /** 当前用户可见性过滤的 deptId：超管返回 null（全量）；
     *  普通用户返回其在 dept_admin 登记的部门（以 dept_admin 为准，aut_user 部门可能滞后不作依据）；
     *  未登记任何部门的管理员返回 -1（SQL 无匹配，仅样例公共可见） */
    private String visibleDeptId(LoginUser loginUser) {
        if (isSuperAdmin(loginUser)) return null;
        if (loginUser == null || !StringUtils.hasText(loginUser.getYyytId())) return "-1";
        Long dept = deptAdminService.deptIdOf(loginUser.getYyytId());
        return dept == null ? "-1" : String.valueOf(dept);
    }

    /** 当前登录人在 dept_admin 登记的部门（未登记返回 null） */
    private Long currentDept(LoginUser loginUser) {
        if (loginUser == null || !StringUtils.hasText(loginUser.getYyytId())) return null;
        return deptAdminService.deptIdOf(loginUser.getYyytId());
    }

    /** 操作权限校验：超管全量；样例仅超管可改；普通任务需为 dept_admin 登记的同一部门（未登记部门者不可操作） */
    private void checkPermission(FlowDispatch d) {
        if (d == null) throw new RuntimeException("任务不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (d.getIsSample() != null && d.getIsSample() == 1) {
            // 样例公开供参考学习：所有人可进入编辑界面查看/试改，但保存落库仅超管允许
            throw new RuntimeException("样例任务仅供学习参考，修改不可保存（仅超管可维护样例）");
        }
        Long dept = currentDept(loginUser);
        if (dept == null) {
            throw new RuntimeException("无权操作其他部门的任务");
        }
        if (d.getDeptId() == null || !Objects.equals(d.getDeptId(), dept)) {
            throw new RuntimeException("无权操作其他部门的任务");
        }
    }

    /** 详情可见性校验（只读）：超管全量；样例公共可见；dept_admin 登记的同一部门可见；否则拒绝 */
    private void checkVisible(FlowDispatch d) {
        if (d == null) throw new RuntimeException("任务不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (d.getIsSample() != null && d.getIsSample() == 1) return;
        Long dept = currentDept(loginUser);
        if (dept == null) {
            throw new RuntimeException("无权查看其他部门的任务");
        }
        if (d.getDeptId() == null || !Objects.equals(d.getDeptId(), dept)) {
            throw new RuntimeException("无权查看其他部门的任务");
        }
    }

    /** 任务分页（含下发配置、期次数、人员数；sample 1样例/0普通/null 全部；progress 进行中/已完成派生；creatorName 创建人；createStart/End 创建时间范围） */
    public PageResult<FlowDispatch> getPage(Integer page, Integer limit, String taskName, String status,
                                            Integer sample, String progress, String creatorName, String createStart, String createEnd) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        String n = StringUtils.hasText(taskName) ? taskName.trim() : null;
        String pr = StringUtils.hasText(progress) ? progress.trim() : null;
        String cn = StringUtils.hasText(creatorName) ? creatorName.trim() : null;
        String deptId = visibleDeptId(SecurityUtils.getLoginUser());
        List<FlowDispatch> list = flowDispatchMapper.selectTaskPage(n, status, sample, pr, cn, createStart, createEnd, deptId, offset, l);
        Long total = flowDispatchMapper.selectTaskCount(n, status, sample, pr, cn, createStart, createEnd, deptId);
        fillCreatorInfo(list);
        return new PageResult<>(list, total);
    }

    /** 任务管理页统计卡（部门可见性过滤后） */
    public DispatchStatsVO getStats() {
        return flowDispatchMapper.selectStats(visibleDeptId(SecurityUtils.getLoginUser()));
    }

    /** 批量补充创建人姓名/部门（按 creatorId 查 sys_user） */
    private void fillCreatorInfo(List<FlowDispatch> list) {
        if (list == null || list.isEmpty()) return;
        List<String> ids = list.stream().map(FlowDispatch::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<String, User> userMap = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getYyytId, u -> u, (a, b) -> a));
        for (FlowDispatch d : list) {
            User u = d.getCreatorId() == null ? null : userMap.get(d.getCreatorId());
            if (u != null) {
                d.setCreatorName(u.getUserName());
                d.setDeptName(u.getDeptName());
            }
        }
    }

    /** 任务详情（编辑回填用，含下发配置） */
    public FlowDispatch getById(String id) {
        if (id == null) return null;
        FlowDispatch task = flowDispatchMapper.selectById(id);
        // 详情可见性校验（超管全量 / 样例公共 / 同部门，否则拒绝）
        checkVisible(task);
        return fillConfig(task);
    }

    /** 启用中的任务列表（部门可见性过滤：样例公共 / 同部门） */
    public List<FlowDispatch> getEnabledList() {
        LambdaQueryWrapper<FlowDispatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDispatch::getStatus, "启用");
        String deptId = visibleDeptId(SecurityUtils.getLoginUser());
        if (deptId != null) {
            wrapper.and(w -> w.eq(FlowDispatch::getIsSample, 1).or().eq(FlowDispatch::getDeptId, deptId));
        }
        wrapper.orderByDesc(FlowDispatch::getCreateTime);
        return flowDispatchMapper.selectList(wrapper);
    }

    /** 创建任务（下发周期配置写入 flow_dispatch_config） */
    @Transactional(rollbackFor = Exception.class)
    public String save(TaskSaveDTO dto) {
        validate(dto);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        checkDeptAdmin(loginUser);
        FlowDispatch task = new FlowDispatch();
        task.setTemplateId(dto.getTemplateId());
        task.setTaskName(dto.getTaskName().trim());
        task.setTaskDesc(dto.getTaskDesc());
        task.setTemplateData(serializeTemplateData(dto.getTemplateData()));
        task.setStatus(normalizeStatus(dto.getStatus()));
        task.setCreatorId(loginUser == null ? null : loginUser.getYyytId());
        task.setCreatorName(loginUser == null ? null : loginUser.getUserName());
        // 部门归属以 dept_admin 登记为准（aut_user 为全量用户表、部门可能滞后）
        task.setDeptId(loginUser == null ? null : deptAdminService.deptIdOf(loginUser.getYyytId()));
        // 样例仅超管通过单独接口设置，普通创建一律为普通任务
        task.setIsSample(0);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        flowDispatchMapper.insert(task);
        saveConfig(task.getId(), dto.getCycleType(), dto.getCycleDay(), dto.getDeadlineDays(), dto.getUrgeDays());
        saveMembers(task.getId(), dto.getMemberIds(), dto.getMemberTaskNames());
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
        task.setStatus(normalizeStatus(dto.getStatus()));
        task.setUpdateTime(new Date());
        flowDispatchMapper.updateById(task);
        saveConfig(dto.getId(), dto.getCycleType(), dto.getCycleDay(), dto.getDeadlineDays(), dto.getUrgeDays());
        if (dto.getMemberIds() != null) {
            saveMembers(dto.getId(), dto.getMemberIds(), dto.getMemberTaskNames());
        }
        return true;
    }

    /** 设置/取消样例：仅超管可操作（样例公共可见、普通用户不可改） */
    public boolean toggleSample(String id) {
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
        // 引用模板校验：模板必须存在、已配置流程节点、已配置表单字段，否则不允许引用并告知用户
        FlowTemplate tpl = flowTemplateMapper.selectById(dto.getTemplateId());
        if (tpl == null) throw new RuntimeException("所选流程模板不存在，请重新选择");
        Long nodeCount = flowTemplateNodeMapper.selectCount(new LambdaQueryWrapper<FlowTemplateNode>()
                .eq(FlowTemplateNode::getTemplateId, dto.getTemplateId()));
        if (nodeCount == null || nodeCount == 0) {
            throw new RuntimeException("该模板「" + tpl.getTemplateName() + "」未配置流程节点，不允许引用，请先在设计模板时添加节点");
        }
        Long fieldCount = flowTemplateFieldMapper.selectCount(new LambdaQueryWrapper<FlowTemplateField>()
                .eq(FlowTemplateField::getTemplateId, dto.getTemplateId()));
        if (fieldCount == null || fieldCount == 0) {
            throw new RuntimeException("该模板「" + tpl.getTemplateName() + "」未配置表单字段，不允许引用，请先在设计模板时添加字段");
        }
        Integer cycle = dto.getCycleType() == null ? CycleType.ONCE.getCode() : dto.getCycleType();
        if (cycle != CycleType.ONCE.getCode() && dto.getCycleDay() == null) throw new RuntimeException("请选择触发日");
    }

    public boolean toggleStatus(String id) {
        FlowDispatch d = flowDispatchMapper.selectById(id);
        if (d == null) return false;
        checkPermission(d);
        d.setStatus(d.getStatus() == null || "停用".equals(d.getStatus()) ? "启用" : "停用");
        d.setUpdateTime(new Date());
        return flowDispatchMapper.updateById(d) > 0;
    }

    /**
     * 修改期次截止时间：同步更新该期次下所有成员任务的截止时间，保证各处展示一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePeriodEndTime(String dispatchId, Date endTime) {
        FlowTaskDispatch d = flowTaskDispatchMapper.selectById(dispatchId);
        if (d == null) throw new RuntimeException("期次不存在");
        if (endTime == null) throw new RuntimeException("请选择截止时间");
        if (d.getStartTime() != null && endTime.before(d.getStartTime())) {
            throw new RuntimeException("截止时间不能早于期次开始时间");
        }
        d.setEndTime(endTime);
        flowTaskDispatchMapper.updateById(d);
        FlowTask upd = new FlowTask();
        upd.setEndTime(endTime);
        upd.setUpdateTime(new Date());
        flowTaskMapper.update(upd, new LambdaQueryWrapper<FlowTask>().eq(FlowTask::getDispatchId, dispatchId));
    }

    /** 期次详情（含计算出的催办时间 = 截止时间 - 提前催办天数），期次人员页展示用 */
    public Map<String, Object> getPeriodInfo(String dispatchId) {
        FlowTaskDispatch d = flowTaskDispatchMapper.selectById(dispatchId);
        if (d == null) throw new RuntimeException("期次不存在");
        FlowDispatchConfig cfg = getConfig(d.getTaskId());
        Integer urgeDays = cfg == null ? null : cfg.getUrgeDays();
        Date urgeTime = null;
        if (d.getEndTime() != null && urgeDays != null && urgeDays > 0) {
            urgeTime = addDays(d.getEndTime(), -urgeDays);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.getId());
        map.put("taskId", d.getTaskId());
        map.put("periodName", d.getPeriodName());
        map.put("taskName", d.getTaskName());
        map.put("startTime", d.getStartTime());
        map.put("endTime", d.getEndTime());
        map.put("urgeDays", urgeDays);
        map.put("urgeTime", urgeTime);
        return map;
    }

    /** 删除任务：仅当任务下无任何期次时允许（同时删除下发配置） */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
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

    /** 创建权限校验：须为 dept_admin 登记的部门管理员（aut_user 部门不作依据） */
    private void checkDeptAdmin(LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("未登录");
        }
        if (deptAdminService.deptIdOf(loginUser.getYyytId()) == null) {
            throw new RuntimeException("非部门管理员，无创建权限");
        }
    }

    /** 状态归一化：null / "1" / "0" / "启用" / "停用" → 中文语义值 */
    private String normalizeStatus(String status) {
        if (status == null || "1".equals(status) || "启用".equals(status)) {
            return "启用";
        }
        return "停用";
    }

    /** 按用户号查姓名（双冗余入库用） */
    private String userNameOf(String yyytId) {
        if (yyytId == null) return null;
        com.company.flow.sys.base.autuser.entity.User u = userMapper.selectById(yyytId);
        return u == null ? null : u.getUserName();
    }

    /** 任务人员列表 */
    public List<TaskMemberInfoVO> getMembers(String taskId) {
        return flowDispatchMapper.selectMembersByTask(taskId);
    }

    /** 全量保存任务人员（后续生成期次抄用；兼容无任务名调用） */
    @Transactional(rollbackFor = Exception.class)
    public void saveMembers(String taskId, List<String> userIds) {
        saveMembers(taskId, userIds, null);
    }

    /** 成员任务名：优先入参指定（userId → 任务名），其次默认「下发给{姓名}的任务」；查无此人返回 null */
    private String memberTaskNameOf(String uid, Map<String, String> names) {
        if (names != null) {
            String n = names.get(uid);
            if (StringUtils.hasText(n)) return n.trim();
        }
        String uname = userNameOf(uid);
        return uname == null ? null : "下发给" + uname + "的任务";
    }

    /** 全量保存任务人员（每位成员带任务名称，生成期次时逐人沿用，实现「任务不按发起人同名混淆」） */
    @Transactional(rollbackFor = Exception.class)
    public void saveMembers(String taskId, List<String> userIds, Map<String, String> taskNames) {
        if (taskId == null) return;
        flowTaskMemberMapper.delete(new LambdaQueryWrapper<FlowTaskMember>().eq(FlowTaskMember::getTaskId, taskId));
        if (userIds == null) return;
        for (String uid : userIds) {
            if (uid == null || uid.trim().isEmpty()) continue;
            FlowTaskMember m = new FlowTaskMember();
            m.setTaskId(taskId);
            m.setUserId(uid);
            m.setUserName(userNameOf(uid));
            m.setTaskName(memberTaskNameOf(uid, taskNames));
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
        if (cycle == CycleType.WEEK.getCode()) {
            int iso = from.getDayOfWeek().getValue(); // Mon=1..Sun=7
            LocalDate monday = from.minusDays(iso - 1);
            if (!current) monday = monday.plusWeeks(1);
            LocalDate start = monday.plusDays(Math.min(day, 7) - 1);
            int year = start.get(WeekFields.ISO.weekBasedYear());
            int weekNo = start.get(WeekFields.ISO.weekOfWeekBasedYear());
            pw.start = start;
            pw.key = year + "-W" + String.format("%02d", weekNo);
            pw.name = year + "年第" + weekNo + "周";
        } else if (cycle == CycleType.MONTH.getCode()) {
            LocalDate first = from.withDayOfMonth(1);
            if (!current) first = first.plusMonths(1);
            LocalDate start = first.withDayOfMonth(Math.min(day, first.lengthOfMonth()));
            String ym = first.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            pw.start = start;
            pw.key = ym;
            pw.name = ym;
        } else { // 每季度
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
    private PeriodWindow nextUndispatchedWindow(int cycle, Integer cycleDay, LocalDate from, boolean firstCurrent, String taskId) {
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
    private boolean isDispatched(String taskId, String periodKey, String periodName) {
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
    public PeriodPreviewVO previewPeriod(String taskId, boolean immediate) {
        PeriodPreviewVO vo = new PeriodPreviewVO();
        FlowDispatch task = taskId == null ? null : flowDispatchMapper.selectById(taskId);
        if (task == null) {
            vo.setStartTime(new Date());
            return vo;
        }
        FlowDispatchConfig cfg = getConfig(taskId);
        if (cfg == null) cfg = new FlowDispatchConfig();
        int cycle = cfg.getCycleType() == null ? CycleType.ONCE.getCode() : cfg.getCycleType();
        Integer day = cfg.getCycleDay();
        vo.setPeriodNo(nextPeriodNo(taskId));
        vo.setUrgeDays(cfg.getUrgeDays());
        LocalDate now = LocalDate.now();
        if (cycle == CycleType.ONCE.getCode()) {
            // 单次下发：期次名由用户填，截止 = 下发日 + deadlineDays
            vo.setPeriodName(null);
            vo.setStartTime(new Date());
            vo.setEndTime(addDays(new Date(), cfg.getDeadlineDays()));
            return vo;
        }
        if (immediate) {
            // 立即下发：当期窗口；已下发则提示，禁止重复
            // 开始/截止时间：先比较配置触发时间与当前时间——
            //   触发日早于/等于今天（补发当期）→ 按下发时间算截止；触发日晚于今天（提前下发当期）→ 按配置触发时间计算
            PeriodWindow cur = nextWindow(cycle, day, now, true);
            vo.setPeriodKey(cur.key);
            vo.setPeriodName(cur.name);
            if (cur.start.isAfter(now)) {
                // 配置触发日未到：按配置时间计算开始与截止
                vo.setStartTime(toDate(cur.start));
                vo.setEndTime(calcEnd(cur.start, cfg.getDeadlineDays()));
            } else {
                // 触发日已过（补发当期）：按下发时间计算开始与截止
                vo.setStartTime(new Date());
                vo.setEndTime(calcEnd(now, cfg.getDeadlineDays()));
            }
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
    public Integer nextPeriodNo(String taskId) {
        if (taskId == null) return null;
        LambdaQueryWrapper<FlowTaskDispatch> pw = new LambdaQueryWrapper<>();
        pw.eq(FlowTaskDispatch::getTaskId, taskId)
          .orderByDesc(FlowTaskDispatch::getPeriodNo)
          .last("LIMIT 1");
        FlowTaskDispatch last = flowTaskDispatchMapper.selectOne(pw);
        return (last == null || last.getPeriodNo() == null) ? 1 : last.getPeriodNo() + 1;
    }

    // ==================== 期次下发检测（到期待下发） ====================

    /**
     * 检索当前是否有任务的期次应该下发：
     * 启用中且非样例的周期任务（周/月/季），其下一个未下发期次窗口开始时间已到（≤ 当前时间）即视为「到期待下发」。
     * 单次（CycleType.ONCE）任务由用户手动触发下发，不参与自动检测。
     */
    public List<DueDispatchVO> checkDueDispatches() {
        List<FlowDispatch> tasks = flowDispatchMapper.selectList(new LambdaQueryWrapper<FlowDispatch>()
                .eq(FlowDispatch::getStatus, "启用")
                .and(w -> w.ne(FlowDispatch::getIsSample, 1).or().isNull(FlowDispatch::getIsSample)));
        Date now = new Date();
        List<DueDispatchVO> result = new ArrayList<>();
        for (FlowDispatch t : tasks) {
            try {
                FlowDispatchConfig cfg = getConfig(t.getId());
                int cycle = cfg == null || cfg.getCycleType() == null ? CycleType.ONCE.getCode() : cfg.getCycleType();
                if (cycle == CycleType.ONCE.getCode()) continue;
                PeriodPreviewVO pv = previewPeriod(t.getId(), false);
                if (pv.getStartTime() != null && !pv.getStartTime().after(now)) {
                    DueDispatchVO vo = new DueDispatchVO();
                    vo.setTaskId(t.getId());
                    vo.setTaskName(t.getTaskName());
                    vo.setPeriodNo(pv.getPeriodNo());
                    vo.setPeriodKey(pv.getPeriodKey());
                    vo.setPeriodName(pv.getPeriodName());
                    vo.setStartTime(pv.getStartTime());
                    vo.setEndTime(pv.getEndTime());
                    result.add(vo);
                }
            } catch (Exception ignored) {
                // 单个任务预览异常不影响其他任务检测
            }
        }
        return result;
    }

    /**
     * 自动下发所有「到期待下发」的期次（复用 generatePeriod 自动周期逻辑）。
     * synchronized 防止与定时任务并发触发时重复下发同一期次。
     *
     * @return 本次实际下发期次数
     */
    public int autoDispatchDuePeriods() {
        synchronized (this) {
            List<DueDispatchVO> due = checkDueDispatches();
            int n = 0;
            for (DueDispatchVO d : due) {
                try {
                    generatePeriod(d.getTaskId(), false, null, false, null, null, null);
                    n++;
                } catch (Exception ignored) {
                    // 单期次下发失败不影响其他期次
                }
            }
            return n;
        }
    }

    // ==================== 期次生成 ====================

    /**
     * 生成期次：自动（按任务下发配置周期 + 是否立即下发）或手动临时期次。
     * 自动下发防重复：当期次已下发（period_key 相同）时禁止再次下发。
     * 期次抄用任务配置：模板、模板配置信息（template_data）、任务名/说明、起止时间、人员（每人一条独立提交任务）。
     * @param memberIds 本期次人员ID（临时指定，可对任务人员做临时增删；为空则抄用任务配置人员）
     * @param manual true=手动临时期次（期次名必填/默认第N期，不占用自动周期编排逻辑）
     * @param manualStartTime 临时期次自定义开始时间（manual=true 且非空时生效；为空则默认当前时间）
     * @param manualEndTime   临时期次自定义截止时间（manual=true 且非空时生效；为空则默认当前时间 + 配置截止天数）
     * @return 生成结果（期次ID、期次名、下次自动下发时间）
     */
    @Transactional(rollbackFor = Exception.class)
    // ==================== 模板创建人字段与任务固化值一致性（防止模板改动后错发旧配置） ====================

    /** 模板当前「创建人填写字段」（模板级：node_id 为空且 field_role=1）ID 集 */
    public Set<String> templateCreatorFieldIds(String templateId) {
        Set<String> out = new HashSet<>();
        if (templateId == null) return out;
        List<FlowTemplateField> fs = flowTemplateFieldMapper.selectList(
                new LambdaQueryWrapper<FlowTemplateField>()
                        .eq(FlowTemplateField::getTemplateId, templateId)
                        .isNull(FlowTemplateField::getNodeId)
                        .ne(FlowTemplateField::getFieldRole, 2)
                        .select(FlowTemplateField::getId));
        for (FlowTemplateField f : fs) out.add(f.getId());
        return out;
    }

    /** 解析 template_data JSON 的键集（fieldId 集） */
    private Set<String> templateDataKeys(String json) {
        Set<String> out = new HashSet<>();
        if (!StringUtils.hasText(json)) return out;
        try {
            Map<String, String> m = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            if (m != null) out.addAll(m.keySet());
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * 任务创建人字段是否已与模板同步：任务 template_data 键集 == 模板创建人字段 ID 集。
     * 任务配置保存时所有创建人字段都会写键（空值写空串），故键集相等即视为已同步。
     */
    public boolean creatorFieldsSynced(FlowDispatch task) {
        if (task == null || task.getTemplateId() == null) return true;
        Set<String> t = templateCreatorFieldIds(task.getTemplateId());
        if (t.isEmpty()) return true;
        return t.equals(templateDataKeys(task.getTemplateData()));
    }

    /**
     * 巡检：找出「模板创建人字段与任务固化值不一致」的启用任务，每天向任务创建人推送一次机器人通知（直到任务重新保存同步）。
     * @return 本轮通知条数
     */
    public int notifyUnsyncedTemplateTasks() {
        List<FlowDispatch> tasks = flowDispatchMapper.selectList(
                new LambdaQueryWrapper<FlowDispatch>()
                        .eq(FlowDispatch::getStatus, "启用")
                        .isNotNull(FlowDispatch::getTemplateId));
        int n = 0;
        for (FlowDispatch t : tasks) {
            try {
                if (t.getIsSample() != null && t.getIsSample() == 1) continue;
                if (creatorFieldsSynced(t)) continue;
                if (t.getCreatorId() == null) continue;
                FlowTemplate tpl = t.getTemplateId() == null ? null : flowTemplateMapper.selectById(t.getTemplateId());
                String tplName = tpl == null ? t.getTemplateId() : tpl.getTemplateName();
                robotService.sendRobot(Collections.singletonList(t.getCreatorId()),
                        "模板字段变更 · 任务待同步", "模板字段同步",
                        "任务「" + t.getTaskName() + "」引用的模板「" + tplName + "」创建人填写字段已有增删/改动，"
                                + "任务配置尚未同步。请打开该任务核对并保存一次；在同步前，手动下发会被阻止、自动下发会跳过该任务。");
                n++;
            } catch (Exception ignored) {
            }
        }
        return n;
    }

        public PeriodGenerateVO generatePeriod(String taskId, boolean immediate, String periodName, boolean manual, List<String> memberIds,
                                           Date manualStartTime, Date manualEndTime) {
            return generatePeriod(taskId, immediate, periodName, manual, memberIds, manualStartTime, manualEndTime, null);
        }

        /** 生成期次（memberTaskNames：本期次人员任务名临时覆盖，仅本期生效，不改任务配置名单；默认通知处理人） */
        public PeriodGenerateVO generatePeriod(String taskId, boolean immediate, String periodName, boolean manual, List<String> memberIds,
                                           Date manualStartTime, Date manualEndTime, Map<String, String> memberTaskNames) {
            return generatePeriod(taskId, immediate, periodName, manual, memberIds, manualStartTime, manualEndTime, memberTaskNames, true);
        }

        /** 生成期次（notifyMembers：是否下发后通知各处理人，手动下发由前端勾选，自动下发默认通知） */
        public PeriodGenerateVO generatePeriod(String taskId, boolean immediate, String periodName, boolean manual, List<String> memberIds,
                                           Date manualStartTime, Date manualEndTime, Map<String, String> memberTaskNames, boolean notifyMembers) {
        FlowDispatch task = flowDispatchMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        if (task.getStatus() != null && "停用".equals(task.getStatus())) {
            throw new RuntimeException("任务已停用，请先启用后再生成期次");
        }
        FlowTemplate tpl = flowTemplateMapper.selectById(task.getTemplateId());
        if (tpl == null) throw new RuntimeException("模板不存在");
        // 一致性闸门：模板创建人字段与任务固化配置不一致时，手动下发直接阻止；
        // 自动下发不重复打扰：仅通知创建人并跳过本期，防止把旧字段配置错发下去（巡检 Job 每天会再提醒）
        if (!creatorFieldsSynced(task)) {
            String tip = "模板「" + tpl.getTemplateName() + "」的创建人填写字段与任务配置不一致（模板字段有增删或改动），"
                    + "请先在任务管理中打开该任务核对并保存一次，再重新下发";
            if (manual) {
                throw new RuntimeException(tip);
            }
            if (task.getCreatorId() != null) {
                robotService.sendRobot(Collections.singletonList(task.getCreatorId()),
                        "自动下发已跳过 · 模板字段待同步", "模板字段同步",
                        "任务「" + task.getTaskName() + "」：" + tip + "。本次自动下发已跳过该任务。");
            }
            throw new RuntimeException("模板创建人字段与任务配置不一致，已跳过自动下发");
        }
        // 开始节点
        LambdaQueryWrapper<FlowTemplateNode> sw = new LambdaQueryWrapper<>();
        sw.eq(FlowTemplateNode::getTemplateId, task.getTemplateId())
          .eq(FlowTemplateNode::getNodeType, NodeType.START.getCode()).last("LIMIT 1");
        FlowTemplateNode firstNode = flowTemplateNodeMapper.selectOne(sw);
        if (firstNode == null) throw new RuntimeException("模板未设计流程节点（缺少开始节点）");
        // 本期次人员：优先用调用方临时指定的人员（临时增删不影响任务配置），否则抄用任务配置人员
        List<String> memberUids;
        if (memberIds != null && !memberIds.isEmpty()) {
            memberUids = memberIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        } else {
            List<FlowTaskMember> members = flowTaskMemberMapper.selectList(
                    new LambdaQueryWrapper<FlowTaskMember>().eq(FlowTaskMember::getTaskId, taskId));
            if (members.isEmpty()) throw new RuntimeException("请先在任务中配置人员");
            memberUids = members.stream().map(FlowTaskMember::getUserId).filter(Objects::nonNull).collect(Collectors.toList());
        }
        if (memberUids.isEmpty()) throw new RuntimeException("请选择本期次人员");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        FlowDispatchConfig cfg = getConfig(taskId);
        if (cfg == null) cfg = new FlowDispatchConfig();
        int cycle = cfg.getCycleType() == null ? CycleType.ONCE.getCode() : cfg.getCycleType();
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
            // 临时期次起止时间：优先用户自定义，未传则沿用默认（当前时间 / 当前时间 + 配置截止天数）
            startTime = manualStartTime != null ? manualStartTime : new Date();
            endTime = manualEndTime != null ? manualEndTime : addDays(new Date(), cfg.getDeadlineDays());
            if (manualStartTime != null && manualEndTime != null && !manualEndTime.after(manualStartTime)) {
                throw new RuntimeException("临时期次截止时间必须晚于开始时间");
            }
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
        if (cycle != CycleType.ONCE.getCode() && nextDispatchTime == null) {
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
        dispatch.setCreatorId(loginUser == null ? null : loginUser.getYyytId());
        dispatch.setCreatorName(loginUser == null ? null : loginUser.getUserName());
        flowTaskDispatchMapper.insert(dispatch);
        // 快照当期模板全部节点配置（含字段定义）到期次节点表，期次内所有成员共享；
        // 模板后续升级不影响本期的节点提示/填写说明/说明文件/字段
        List<FlowTaskDispatchNode> snapNodes = snapshotTemplateNodes(dispatch.getId(), task.getTemplateId());
        // 快照模板级字段定义（node_id 为空：创建人字段 + 未绑定节点的处理人字段）
        List<FlowTemplateField> tplFields = flowTemplateFieldMapper.selectList(
                new LambdaQueryWrapper<FlowTemplateField>()
                        .eq(FlowTemplateField::getTemplateId, task.getTemplateId())
                        .isNull(FlowTemplateField::getNodeId)
                        .orderByAsc(FlowTemplateField::getSortNum));
        try {
            dispatch.setTemplateFieldsJson(tplFields.isEmpty() ? null : objectMapper.writeValueAsString(tplFields));
            flowTaskDispatchMapper.updateById(dispatch);
        } catch (Exception ignored) {
        }
        FlowTaskDispatchNode firstSnap = snapNodes.isEmpty() ? null : snapNodes.get(0);
        if (firstSnap == null) throw new RuntimeException("模板未设计流程节点（缺少开始节点）");
        // 成员级任务名称（flow_task_member.task_name：任务管理配置名单时按人定义，每期沿用，
        // 任务处理/回看时靠任务名区分不同成员的任务，而不是都叫计划任务名）
        Map<String, String> memberNameMap = new HashMap<>();
        for (FlowTaskMember fm : flowTaskMemberMapper.selectList(
                new LambdaQueryWrapper<FlowTaskMember>().eq(FlowTaskMember::getTaskId, taskId))) {
            if (fm.getUserId() != null && StringUtils.hasText(fm.getTaskName())) {
                memberNameMap.put(fm.getUserId(), fm.getTaskName().trim());
            }
        }
        // 本期次人员任务名临时覆盖（生成期次弹窗里可改，仅本期生效，不改任务配置名单）
        if (memberTaskNames != null) {
            for (Map.Entry<String, String> en : memberTaskNames.entrySet()) {
                if (en.getKey() != null && en.getValue() != null && StringUtils.hasText(en.getValue().trim())) {
                    memberNameMap.put(en.getKey(), en.getValue().trim());
                }
            }
        }
        // 为每位人员创建独立提交任务（从开始节点重新走流程；任务名取该成员任务名，无配置默认「下发给{姓名}的任务」）
        for (String uid : memberUids) {
            FlowTask ft = new FlowTask();
            ft.setTemplateId(task.getTemplateId());
            String memberTname = memberNameMap.get(uid);
            if (!StringUtils.hasText(memberTname)) memberTname = memberTaskNameOf(uid, null);
            ft.setTaskName(StringUtils.hasText(memberTname) ? memberTname : task.getTaskName());
            ft.setTaskDesc(task.getTaskDesc());
            ft.setTemplateData(task.getTemplateData());
            ft.setStartTime(startTime);
            ft.setEndTime(endTime);
            ft.setStatus("进行中");
            ft.setTemplateVersion(tpl.getVersion());
            ft.setCurrentNodeId(firstSnap.getId());
            ft.setCurrentHandlerId(uid);
            ft.setFinishedNodeCount(0);
            ft.setTotalNodeCount(snapNodes.size());
            ft.setCreatorId(dispatch.getCreatorId());
            ft.setCreatorName(dispatch.getCreatorName());
            ft.setDispatchId(dispatch.getId());
            flowTaskMapper.insert(ft);
            FlowTaskNode n = new FlowTaskNode();
            n.setTaskId(ft.getId());
            n.setNodeId(firstSnap.getId());
            n.setDispatchNodeId(firstSnap.getId());
            n.setNodeName(firstSnap.getNodeName());
            n.setSortNum(firstSnap.getSortNum());
            n.setNodeType(firstSnap.getNodeType());
            n.setHandlerUserId(uid);
            n.setHandlerUserName(uid == null ? null : userNameOf(uid));
            n.setSubmitStatus(0);
            n.setAction(0);
            flowTaskNodeMapper.insert(n);
        }
        // 下发后通知各处理人（手动下发可取消勾选；自动下发默认通知）
        if (notifyMembers) {
            flowNotifyService.notifyDispatch(task.getTaskName(), periodNameFinal, memberUids);
        }
        PeriodGenerateVO vo = new PeriodGenerateVO();
        vo.setPeriodId(dispatch.getId());
        vo.setPeriodName(periodNameFinal);
        vo.setNextDispatchTime(nextDispatchTime);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public int addMembersToDispatch(String dispatchId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) throw new RuntimeException("请选择要新增的人员");
        List<String> uids = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (uids.isEmpty()) throw new RuntimeException("请选择要新增的人员");
        FlowTaskDispatch dispatch = flowTaskDispatchMapper.selectById(dispatchId);
        if (dispatch == null) throw new RuntimeException("期次不存在");
        // 期次节点快照（优先，保证与期次锁定版本一致）；存量期次无快照则降级查当前模板
        List<FlowTaskDispatchNode> snapNodes = flowTaskDispatchNodeMapper.selectList(
                new LambdaQueryWrapper<FlowTaskDispatchNode>()
                        .eq(FlowTaskDispatchNode::getDispatchId, dispatchId)
                        .orderByAsc(FlowTaskDispatchNode::getSortNum));
        FlowTaskDispatchNode firstSnap = snapNodes.isEmpty() ? null : snapNodes.get(0);
        String firstNodeId;
        String firstNodeName;
        Integer firstNodeSort;
        Integer firstNodeType;
        int total;
        if (firstSnap != null) {
            firstNodeId = firstSnap.getId();
            firstNodeName = firstSnap.getNodeName();
            firstNodeSort = firstSnap.getSortNum();
            firstNodeType = firstSnap.getNodeType();
            total = snapNodes.size();
        } else {
            FlowTemplate tpl = flowTemplateMapper.selectById(dispatch.getTemplateId());
            if (tpl == null) throw new RuntimeException("模板不存在");
            LambdaQueryWrapper<FlowTemplateNode> sw = new LambdaQueryWrapper<>();
            sw.eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId())
              .eq(FlowTemplateNode::getNodeType, NodeType.START.getCode()).last("LIMIT 1");
            FlowTemplateNode firstNode = flowTemplateNodeMapper.selectOne(sw);
            if (firstNode == null) throw new RuntimeException("模板未设计流程节点（缺少开始节点）");
            firstNodeId = firstNode.getId();
            firstNodeName = firstNode.getNodeName();
            firstNodeSort = firstNode.getSortNum();
            firstNodeType = firstNode.getNodeType();
            total = flowTemplateNodeMapper.selectCount(
                    new LambdaQueryWrapper<FlowTemplateNode>().eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId())).intValue();
        }
        // 该期次已存在的人员（从节点处理人判定，避免重复新增）
        List<String> taskIds = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>().eq(FlowTask::getDispatchId, dispatchId))
                .stream().map(FlowTask::getId).collect(Collectors.toList());
        List<String> existing = new ArrayList<>();
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
        for (String uid : uids) {
            if (existing.contains(uid)) continue;
            FlowTask ft = new FlowTask();
            ft.setTemplateId(dispatch.getTemplateId());
            ft.setDispatchId(dispatchId);
            String memberTname = memberTaskNameOf(uid, null);
            ft.setTaskName(StringUtils.hasText(memberTname) ? memberTname : dispatch.getTaskName());
            ft.setTaskDesc(dispatch.getTaskDesc());
            ft.setTemplateData(dispatch.getTemplateData());
            ft.setStartTime(dispatch.getStartTime());
            ft.setEndTime(dispatch.getEndTime());
            ft.setStatus("进行中");
            ft.setTemplateVersion(dispatch.getTemplateVersion());
            ft.setCurrentNodeId(firstNodeId);
            ft.setCurrentHandlerId(uid);
            ft.setFinishedNodeCount(0);
            ft.setTotalNodeCount(total);
            ft.setCreatorId(dispatch.getCreatorId());
            flowTaskMapper.insert(ft);
            FlowTaskNode n = new FlowTaskNode();
            n.setTaskId(ft.getId());
            n.setNodeId(firstNodeId);
            n.setDispatchNodeId(firstSnap == null ? null : firstSnap.getId());
            n.setNodeName(firstNodeName);
            n.setSortNum(firstNodeSort);
            n.setNodeType(firstNodeType);
            n.setHandlerUserId(uid);
            n.setHandlerUserName(uid == null ? null : userNameOf(uid));
            n.setSubmitStatus(0);
            n.setAction(0);
            flowTaskNodeMapper.insert(n);
            count++;
        }
        if (count == 0) throw new RuntimeException("所选人员均已在该期次中，无需重复新增");
        // 补人后通知新增处理人（接收人 = 实际新增者）
        List<String> addedIds = uids.stream().filter(uid -> !existing.contains(uid)).collect(Collectors.toList());
        if (!addedIds.isEmpty()) {
            flowNotifyService.notifyDispatch(dispatch.getTaskName(), null, addedIds);
        }
        return count;
    }

    /**
     * 期次下发时快照当期模板全部节点配置（含字段定义）到 flow_task_dispatch_node。
     * 期次内所有成员共享；后续模板升级/删除不影响历史期次；临时人员新增复用该快照。
     * @return 快照节点列表（按 sort_num 升序）
     */
    private List<FlowTaskDispatchNode> snapshotTemplateNodes(String dispatchId, String templateId) {
        List<FlowTemplateNode> nodes = flowTemplateNodeMapper.selectList(
                new LambdaQueryWrapper<FlowTemplateNode>()
                        .eq(FlowTemplateNode::getTemplateId, templateId)
                        .orderByAsc(FlowTemplateNode::getSortNum));
        List<FlowTaskDispatchNode> snaps = new ArrayList<>();
        for (FlowTemplateNode nd : nodes) {
            FlowTaskDispatchNode s = new FlowTaskDispatchNode();
            s.setDispatchId(dispatchId);
            s.setNodeId(nd.getId());
            s.setNodeName(nd.getNodeName());
            s.setSortNum(nd.getSortNum());
            s.setNodeType(nd.getNodeType());
            s.setNodeTips(nd.getNodeTips());
            s.setGuideText(nd.getGuideText());
            // 说明文件复制为期次维度附件（防模板删除文件导致历史期次悬空引用）
            s.setGuideFiles(copyGuideFilesForDispatch(dispatchId, nd.getGuideFiles()));
            s.setNextHandlerTip(nd.getNextHandlerTip());
            s.setBranchConfig(nd.getBranchConfig());
            // 字段定义快照：该节点字段 + 绑定该节点的处理人字段（fieldRole=2）
            List<FlowTemplateField> fields = flowTemplateFieldMapper.selectList(
                    new LambdaQueryWrapper<FlowTemplateField>()
                            .eq(FlowTemplateField::getTemplateId, templateId)
                            .and(w -> w.eq(FlowTemplateField::getNodeId, nd.getId())
                                    .or().eq(FlowTemplateField::getBindNodeId, nd.getId()))
                            .orderByAsc(FlowTemplateField::getSortNum));
            try {
                s.setFieldsJson(fields.isEmpty() ? null : objectMapper.writeValueAsString(fields));
            } catch (Exception e) {
                s.setFieldsJson(null);
            }
            flowTaskDispatchNodeMapper.insert(s);
            snaps.add(s);
        }
        return snaps;
    }

    /**
     * 期次说明文件复制：把模板节点的 guide_files 每条复制为「期次维度」附件记录（新 attachId），
     * 返回新 JSON 写入快照。模板中删除/替换说明文件不再影响历史期次；无文件或复制失败时原样返回。
     */
    private String copyGuideFilesForDispatch(String dispatchId, String guideFilesJson) {
        if (!StringUtils.hasText(guideFilesJson)) return guideFilesJson;
        try {
            List<Map<String, Object>> files = objectMapper.readValue(
                    guideFilesJson, new TypeReference<List<Map<String, Object>>>() {});
            if (files == null || files.isEmpty()) return guideFilesJson;
            String bizId = "node-guide-dispatch-" + dispatchId;
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<Map<String, Object>> copied = new ArrayList<>();
            for (Map<String, Object> f : files) {
                Attach src = new Attach();
                src.setAttachId(f.get("attachId") == null ? null : String.valueOf(f.get("attachId")));
                src.setFileName(f.get("fileName") == null ? null : String.valueOf(f.get("fileName")));
                src.setEcsUrl(f.get("ecsUrl") == null ? null : String.valueOf(f.get("ecsUrl")));
                src.setCreator(f.get("creator") == null ? null : String.valueOf(f.get("creator")));
                if (f.get("createTime") != null) {
                    try { src.setCreateTime(fmt.parse(String.valueOf(f.get("createTime")))); } catch (Exception ignore) { /* 忽略解析失败 */ }
                }
                Attach copy = attachService.copyForSnapshot(src, bizId);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("attachId", copy.getAttachId());
                item.put("bizId", copy.getBizId());
                item.put("fileName", copy.getFileName());
                item.put("ecsUrl", copy.getEcsUrl());
                item.put("creator", copy.getCreator());
                if (copy.getCreateTime() != null) item.put("createTime", fmt.format(copy.getCreateTime()));
                if (copy.getModifiedTime() != null) item.put("modifiedTime", fmt.format(copy.getModifiedTime()));
                copied.add(item);
            }
            return objectMapper.writeValueAsString(copied);
        } catch (Exception e) {
            log.warn("期次说明文件复制失败，保留原 guide_files：{}", e.getMessage());
            return guideFilesJson;
        }
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

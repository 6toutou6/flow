package com.company.flow.sys.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.flow.sys.flowtask.entity.FlowDispatchConfigTemplate;
import com.company.flow.sys.flowtask.mapper.FlowDispatchConfigTemplateMapper;
import com.company.flow.sys.flowtask.vo.ConfigTemplateStatsVO;
import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.mapper.UserMapper;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 下发配置模板：增删改查，供新建/编辑任务时一键拉取复用。
 * 可见性：超管全量；普通用户样例公共可见 + 同部门创建；样例仅超管可改。
 */
@Service
public class FlowDispatchConfigTemplateService {

    @Autowired
    private FlowDispatchConfigTemplateMapper mapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private com.company.flow.sys.base.deptAdmin.service.DeptAdminService deptAdminService;

    // ==================== 可见性与权限 ====================

    private boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
    }

    /** 当前登录人在 dept_admin 登记的部门（超管返回 null 全量；未登记返回 -1，仅样例/自建可见） */
    private Long visibleDeptId(LoginUser loginUser) {
        if (isSuperAdmin(loginUser)) return null;
        if (loginUser == null || !StringUtils.hasText(loginUser.getYyytId())) return -1L;
        Long dept = deptAdminService.deptIdOf(loginUser.getYyytId());
        return dept == null ? -1L : dept;
    }

    /** 操作权限校验：超管全量；样例仅超管可改；自己创建的可改；其余需为 dept_admin 登记的同一部门 */
    private void checkPermission(FlowDispatchConfigTemplate tpl) {
        if (tpl == null) throw new RuntimeException("配置模板不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (tpl.getIsSample() != null && tpl.getIsSample() == 1) {
            throw new RuntimeException("样例配置模板仅超管可修改");
        }
        if (loginUser == null) throw new RuntimeException("无权操作该配置模板");
        if (tpl.getCreatorId() != null && Objects.equals(tpl.getCreatorId(), loginUser.getYyytId())) return;
        Long dept = deptAdminService.deptIdOf(loginUser.getYyytId());
        if (dept == null) {
            throw new RuntimeException("无权操作其他部门的配置模板");
        }
        if (tpl.getDeptId() == null || !Objects.equals(tpl.getDeptId(), dept)) {
            throw new RuntimeException("无权操作其他部门的配置模板");
        }
    }

    /** 详情可见性校验（只读）：超管全量；样例公共可见；自己创建的可见；dept_admin 登记的同一部门可见；否则拒绝 */
    private void checkVisible(FlowDispatchConfigTemplate tpl) {
        if (tpl == null) throw new RuntimeException("配置模板不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (tpl.getIsSample() != null && tpl.getIsSample() == 1) return;
        if (loginUser == null) throw new RuntimeException("无权查看该配置模板");
        if (tpl.getCreatorId() != null && Objects.equals(tpl.getCreatorId(), loginUser.getYyytId())) return;
        Long dept = deptAdminService.deptIdOf(loginUser.getYyytId());
        if (dept == null) {
            throw new RuntimeException("无权查看其他部门的配置模板");
        }
        if (tpl.getDeptId() == null || !Objects.equals(tpl.getDeptId(), dept)) {
            throw new RuntimeException("无权查看其他部门的配置模板");
        }
    }

    /** 列表（可见性过滤 + 分类：样例/我创建/周期/创建人/更新时间，可叠加 + 按创建时间倒序，配置数量有限无需分页） */
    public List<FlowDispatchConfigTemplate> list(String keyword, Integer sample, Boolean mine, Integer cycleType,
                                                 String creatorName, String updateStart, String updateEnd) {
        LambdaQueryWrapper<FlowDispatchConfigTemplate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(FlowDispatchConfigTemplate::getConfigName, keyword.trim())
                          .or().like(FlowDispatchConfigTemplate::getRemark, keyword.trim()));
        }
        if (cycleType != null) {
            qw.eq(FlowDispatchConfigTemplate::getCycleType, cycleType);
        }
        if (sample != null) {
            qw.eq(FlowDispatchConfigTemplate::getIsSample, sample);
        }
        if (creatorName != null && !creatorName.trim().isEmpty()) {
            qw.like(FlowDispatchConfigTemplate::getCreatorName, creatorName.trim());
        }
        if (StringUtils.hasText(updateStart)) {
            qw.ge(FlowDispatchConfigTemplate::getUpdateTime, updateStart.trim());
        }
        if (StringUtils.hasText(updateEnd)) {
            qw.le(FlowDispatchConfigTemplate::getUpdateTime, updateEnd.trim() + " 23:59:59");
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long deptId = visibleDeptId(loginUser);
        String userId = loginUser == null ? null : loginUser.getYyytId();
        if (deptId != null) {
            // 样例公共可见 或 同部门创建 或 自己创建
            qw.and(w -> w.eq(FlowDispatchConfigTemplate::getIsSample, 1)
                          .or().eq(FlowDispatchConfigTemplate::getDeptId, deptId)
                          .or().eq(FlowDispatchConfigTemplate::getCreatorId, userId));
        }
        if (Boolean.TRUE.equals(mine)) {
            qw.eq(FlowDispatchConfigTemplate::getCreatorId, userId);
        }
        qw.orderByDesc(FlowDispatchConfigTemplate::getId);
        List<FlowDispatchConfigTemplate> list = mapper.selectList(qw);
        fillCreatorInfo(list);
        return list;
    }

    /** 配置模板页统计卡（与列表同可见性口径） */
    public ConfigTemplateStatsVO stats() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long deptId = visibleDeptId(loginUser);
        String userId = loginUser == null ? null : loginUser.getYyytId();
        ConfigTemplateStatsVO vo = new ConfigTemplateStatsVO();
        LambdaQueryWrapper<FlowDispatchConfigTemplate> totalQw = new LambdaQueryWrapper<>();
        if (deptId != null) {
            totalQw.and(w -> w.eq(FlowDispatchConfigTemplate::getIsSample, 1)
                              .or().eq(FlowDispatchConfigTemplate::getDeptId, deptId)
                              .or().eq(FlowDispatchConfigTemplate::getCreatorId, userId));
        }
        vo.setTotal(mapper.selectCount(totalQw));
        // 样例公共可见，恒全量统计
        vo.setSampleCount(mapper.selectCount(new LambdaQueryWrapper<FlowDispatchConfigTemplate>()
                .eq(FlowDispatchConfigTemplate::getIsSample, 1)));
        vo.setMineCount(userId == null ? 0L
                : mapper.selectCount(new LambdaQueryWrapper<FlowDispatchConfigTemplate>()
                        .eq(FlowDispatchConfigTemplate::getCreatorId, userId)));
        return vo;
    }

    /** 批量补充创建人姓名/部门 */
    private void fillCreatorInfo(List<FlowDispatchConfigTemplate> list) {
        if (list == null || list.isEmpty()) return;
        List<String> ids = list.stream().map(FlowDispatchConfigTemplate::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<String, User> userMap = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getYyytId, u -> u, (a, b) -> a));
        for (FlowDispatchConfigTemplate t : list) {
            User u = t.getCreatorId() == null ? null : userMap.get(t.getCreatorId());
            if (u != null) {
                t.setCreatorName(u.getUserName());
                t.setDeptName(u.getDeptName());
            }
        }
    }

    public FlowDispatchConfigTemplate getById(String id) {
        if (id == null) return null;
        FlowDispatchConfigTemplate tpl = mapper.selectById(id);
        // 详情可见性校验（超管全量 / 样例公共 / 同部门，否则拒绝）
        checkVisible(tpl);
        return tpl;
    }

    /** 新增或更新（存在 id 则更新）；新建归属当前用户/部门，样例仅超管通过单独接口设置 */
    public FlowDispatchConfigTemplate save(FlowDispatchConfigTemplate tpl) {
        Date now = new Date();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (tpl.getId() == null) {
            // 新建归属部门管理员（dept_admin 登记），非管理员不可创建
            if (loginUser != null && !isSuperAdmin(loginUser)
                    && deptAdminService.deptIdOf(loginUser.getYyytId()) == null) {
                throw new RuntimeException("非部门管理员，无创建权限");
            }
            tpl.setCreatorId(loginUser == null ? null : loginUser.getYyytId());
        tpl.setCreatorName(loginUser == null ? null : loginUser.getUserName());
            // 部门归属以 dept_admin 登记为准（aut_user 为全量用户表、部门可能滞后）
            tpl.setDeptId(loginUser == null ? null : deptAdminService.deptIdOf(loginUser.getYyytId()));
            tpl.setIsSample(0);
            tpl.setCreateTime(now);
            tpl.setUpdateTime(now);
            mapper.insert(tpl);
        } else {
            checkPermission(mapper.selectById(tpl.getId()));
            tpl.setUpdateTime(now);
            mapper.updateById(tpl);
        }
        return tpl;
    }

    /** 设置/取消样例：仅超管可操作 */
    public boolean toggleSample(String id) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!isSuperAdmin(loginUser)) throw new RuntimeException("仅超管可设置样例");
        FlowDispatchConfigTemplate tpl = mapper.selectById(id);
        if (tpl == null) return false;
        tpl.setIsSample(tpl.getIsSample() == null || tpl.getIsSample() == 0 ? 1 : 0);
        tpl.setUpdateTime(new Date());
        return mapper.updateById(tpl) > 0;
    }

    /** 删除模板：不影响已拉取到任务上的配置 */
    public boolean delete(String id) {
        checkPermission(mapper.selectById(id));
        return id != null && mapper.deleteById(id) > 0;
    }

    /** 复制配置模板：样例可复制（公共）；复制产物归属当前部门管理员（dept_admin 登记），样例复制后转为普通配置 */
    public FlowDispatchConfigTemplate copy(String id) {
        FlowDispatchConfigTemplate src = mapper.selectById(id);
        if (src == null) throw new RuntimeException("配置模板不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) throw new RuntimeException("未登录");
        Long myDept = deptAdminService.deptIdOf(loginUser.getYyytId());
        if (myDept == null) {
            throw new RuntimeException("非部门管理员，无复制权限");
        }
        Date now = new Date();
        FlowDispatchConfigTemplate c = new FlowDispatchConfigTemplate();
        c.setConfigName(src.getConfigName() + "_副本");
        c.setCycleType(src.getCycleType());
        c.setCycleDay(src.getCycleDay());
        c.setDeadlineDays(src.getDeadlineDays());
        c.setUrgeDays(src.getUrgeDays());
        c.setRemark(src.getRemark());
        c.setDeptId(myDept);
        c.setIsSample(0);
        c.setCreatorId(loginUser.getYyytId());
        c.setCreatorName(loginUser.getUserName());
        c.setCreateTime(now);
        c.setUpdateTime(now);
        mapper.insert(c);
        return c;
    }
}

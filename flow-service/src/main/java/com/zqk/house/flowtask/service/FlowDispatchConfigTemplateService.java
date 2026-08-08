package com.zqk.house.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zqk.house.flowtask.entity.FlowDispatchConfigTemplate;
import com.zqk.house.flowtask.mapper.FlowDispatchConfigTemplateMapper;
import com.zqk.house.flowtask.vo.ConfigTemplateStatsVO;
import com.zqk.house.sysuser.entity.SysUser;
import com.zqk.house.sysuser.mapper.SysUserMapper;
import com.zqk.house.sysuser.vo.LoginUser;
import com.zqk.house.util.SecurityUtils;
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
    private SysUserMapper sysUserMapper;

    // ==================== 可见性与权限 ====================

    private boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
    }

    private Long visibleDeptId(LoginUser loginUser) {
        if (isSuperAdmin(loginUser)) return null;
        if (loginUser == null || loginUser.getDeptId() == null) return -1L;
        return loginUser.getDeptId();
    }

    /** 操作权限校验：超管全量；样例仅超管可改；自己创建的可改；其余需同部门（无部门用户不可操作） */
    private void checkPermission(FlowDispatchConfigTemplate tpl) {
        if (tpl == null) throw new RuntimeException("配置模板不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (tpl.getIsSample() != null && tpl.getIsSample() == 1) {
            throw new RuntimeException("样例配置模板仅超管可修改");
        }
        if (loginUser == null) throw new RuntimeException("无权操作该配置模板");
        if (tpl.getCreatorId() != null && Objects.equals(tpl.getCreatorId(), loginUser.getId())) return;
        if (loginUser.getDeptId() == null) {
            throw new RuntimeException("无权操作其他部门的配置模板");
        }
        if (tpl.getDeptId() == null || !Objects.equals(tpl.getDeptId(), loginUser.getDeptId())) {
            throw new RuntimeException("无权操作其他部门的配置模板");
        }
    }

    /** 详情可见性校验（只读）：超管全量；样例公共可见；自己创建的可见；同部门可见；否则拒绝 */
    private void checkVisible(FlowDispatchConfigTemplate tpl) {
        if (tpl == null) throw new RuntimeException("配置模板不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (tpl.getIsSample() != null && tpl.getIsSample() == 1) return;
        if (loginUser == null) throw new RuntimeException("无权查看该配置模板");
        if (tpl.getCreatorId() != null && Objects.equals(tpl.getCreatorId(), loginUser.getId())) return;
        if (loginUser.getDeptId() == null) {
            throw new RuntimeException("无权查看其他部门的配置模板");
        }
        if (tpl.getDeptId() == null || !Objects.equals(tpl.getDeptId(), loginUser.getDeptId())) {
            throw new RuntimeException("无权查看其他部门的配置模板");
        }
    }

    /** 列表（可见性过滤 + 按创建时间倒序，配置数量有限无需分页） */
    public List<FlowDispatchConfigTemplate> list(String keyword) {
        LambdaQueryWrapper<FlowDispatchConfigTemplate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(FlowDispatchConfigTemplate::getConfigName, keyword.trim())
                          .or().like(FlowDispatchConfigTemplate::getRemark, keyword.trim()));
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long deptId = visibleDeptId(loginUser);
        Long userId = loginUser == null ? null : loginUser.getId();
        if (deptId != null) {
            // 样例公共可见 或 同部门创建 或 自己创建
            qw.and(w -> w.eq(FlowDispatchConfigTemplate::getIsSample, 1)
                          .or().eq(FlowDispatchConfigTemplate::getDeptId, deptId)
                          .or().eq(FlowDispatchConfigTemplate::getCreatorId, userId));
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
        Long userId = loginUser == null ? null : loginUser.getId();
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
        List<Long> ids = list.stream().map(FlowDispatchConfigTemplate::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        for (FlowDispatchConfigTemplate t : list) {
            SysUser u = t.getCreatorId() == null ? null : userMap.get(t.getCreatorId());
            if (u != null) {
                t.setCreatorName(u.getRealName());
                t.setDeptName(u.getDeptName());
            }
        }
    }

    public FlowDispatchConfigTemplate getById(Long id) {
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
            tpl.setCreatorId(loginUser == null ? null : loginUser.getId());
            tpl.setDeptId(loginUser == null ? null : loginUser.getDeptId());
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
    public boolean toggleSample(Long id) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!isSuperAdmin(loginUser)) throw new RuntimeException("仅超管可设置样例");
        FlowDispatchConfigTemplate tpl = mapper.selectById(id);
        if (tpl == null) return false;
        tpl.setIsSample(tpl.getIsSample() == null || tpl.getIsSample() == 0 ? 1 : 0);
        tpl.setUpdateTime(new Date());
        return mapper.updateById(tpl) > 0;
    }

    /** 删除模板：不影响已拉取到任务上的配置 */
    public boolean delete(Long id) {
        checkPermission(mapper.selectById(id));
        return id != null && mapper.deleteById(id) > 0;
    }
}

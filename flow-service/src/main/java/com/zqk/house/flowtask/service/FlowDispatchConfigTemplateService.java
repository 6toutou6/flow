package com.zqk.house.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zqk.house.flowtask.entity.FlowDispatchConfigTemplate;
import com.zqk.house.flowtask.mapper.FlowDispatchConfigTemplateMapper;
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
        return isSuperAdmin(loginUser) ? null : (loginUser == null ? null : loginUser.getDeptId());
    }

    private void checkPermission(FlowDispatchConfigTemplate tpl) {
        if (tpl == null) throw new RuntimeException("配置模板不存在");
        if (isSuperAdmin(SecurityUtils.getLoginUser())) return;
        if (tpl.getIsSample() != null && tpl.getIsSample() == 1) {
            throw new RuntimeException("样例配置模板仅超管可修改");
        }
        if (tpl.getDeptId() != null) {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null || !Objects.equals(tpl.getDeptId(), loginUser.getDeptId())) {
                throw new RuntimeException("无权操作其他部门的配置模板");
            }
        }
    }

    /** 列表（可见性过滤 + 按创建时间倒序，配置数量有限无需分页） */
    public List<FlowDispatchConfigTemplate> list(String keyword) {
        LambdaQueryWrapper<FlowDispatchConfigTemplate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(FlowDispatchConfigTemplate::getConfigName, keyword.trim())
              .or().like(FlowDispatchConfigTemplate::getRemark, keyword.trim());
        }
        Long deptId = visibleDeptId(SecurityUtils.getLoginUser());
        if (deptId != null) {
            qw.and(w -> w.eq(FlowDispatchConfigTemplate::getIsSample, 1)
                          .or().eq(FlowDispatchConfigTemplate::getDeptId, deptId));
        }
        qw.orderByDesc(FlowDispatchConfigTemplate::getId);
        List<FlowDispatchConfigTemplate> list = mapper.selectList(qw);
        fillCreatorInfo(list);
        return list;
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
        return id == null ? null : mapper.selectById(id);
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

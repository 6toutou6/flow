package com.company.flow.sys.base.deptAdmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.flow.sys.base.deptAdmin.entity.DeptAdmin;
import com.company.flow.sys.base.deptAdmin.mapper.DeptAdminMapper;
import com.company.flow.sys.base.deptAdmin.service.DeptAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 部门管理员服务实现
 */
@Service
public class DeptAdminServiceImpl implements DeptAdminService {

    @Autowired
    private DeptAdminMapper deptAdminMapper;

    @Override
    public List<DeptAdmin> listAll() {
        return deptAdminMapper.selectList(new LambdaQueryWrapper<DeptAdmin>()
                .orderByAsc(DeptAdmin::getDeptId).orderByAsc(DeptAdmin::getAdminYstId));
    }

    @Override
    public boolean add(DeptAdmin admin) {
        if (!StringUtils.hasText(admin.getAdminYstId()) || !StringUtils.hasText(admin.getDeptId())) {
            return false;
        }
        // 唯一键 (admin_yst_id, dept_id) 冲突则忽略
        long exist = deptAdminMapper.selectCount(new LambdaQueryWrapper<DeptAdmin>()
                .eq(DeptAdmin::getAdminYstId, admin.getAdminYstId())
                .eq(DeptAdmin::getDeptId, admin.getDeptId()));
        if (exist > 0) {
            return false;
        }
        return deptAdminMapper.insert(admin) > 0;
    }

    @Override
    public boolean update(DeptAdmin admin) {
        if (!StringUtils.hasText(admin.getAdminYstId()) || !StringUtils.hasText(admin.getDeptId())) {
            return false;
        }
        return deptAdminMapper.update(admin, new LambdaQueryWrapper<DeptAdmin>()
                .eq(DeptAdmin::getAdminYstId, admin.getAdminYstId())
                .eq(DeptAdmin::getDeptId, admin.getDeptId())) > 0;
    }

    @Override
    public boolean delete(String adminYstId, String deptId) {
        return deptAdminMapper.delete(new LambdaQueryWrapper<DeptAdmin>()
                .eq(DeptAdmin::getAdminYstId, adminYstId)
                .eq(DeptAdmin::getDeptId, deptId)) > 0;
    }

    @Override
    public boolean isDeptAdmin(String yyytId, Long deptId) {
        if (!StringUtils.hasText(yyytId) || deptId == null) {
            return false;
        }
        return deptAdminMapper.selectCount(new LambdaQueryWrapper<DeptAdmin>()
                .eq(DeptAdmin::getAdminYstId, yyytId)
                .eq(DeptAdmin::getDeptId, String.valueOf(deptId))) > 0;
    }

    @Override
    public boolean currentIsDeptAdmin(String yyytId, Long deptId) {
        return isDeptAdmin(yyytId, deptId);
    }

    @Override
    public List<Map<String, String>> deptOptions() {
        // 按 dept_admin 表已有部门去重（部门信息以 dept_admin 为准；数据量小，代码去重更稳）
        List<DeptAdmin> list = deptAdminMapper.selectList(new LambdaQueryWrapper<DeptAdmin>()
                .orderByAsc(DeptAdmin::getDeptId));
        Map<String, String> seen = new java.util.LinkedHashMap<>();
        for (DeptAdmin d : list) {
            if (d.getDeptId() != null && !seen.containsKey(d.getDeptId())) {
                seen.put(d.getDeptId(), d.getDeptName());
            }
        }
        List<Map<String, String>> options = new java.util.ArrayList<>();
        seen.forEach((id, name) -> {
            Map<String, String> m = new java.util.HashMap<>();
            m.put("deptId", id);
            m.put("deptName", name);
            options.add(m);
        });
        return options;
    }
}

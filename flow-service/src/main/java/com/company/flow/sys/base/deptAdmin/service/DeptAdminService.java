package com.company.flow.sys.base.deptAdmin.service;

import com.company.flow.sys.base.deptAdmin.entity.DeptAdmin;

import java.util.List;
import java.util.Map;

/**
 * 部门管理员服务接口
 */
public interface DeptAdminService {

    /** 全部部门管理员 */
    List<DeptAdmin> listAll();

    /** 新增部门管理员 */
    boolean add(DeptAdmin admin);

    /** 更新部门管理员（按 adminYstId + deptId 联合主键） */
    boolean update(DeptAdmin admin);

    /** 删除部门管理员（按用户号+部门） */
    boolean delete(String adminYstId, String deptId);

    /** 判断某用户是否为指定部门的部门管理员 */
    boolean isDeptAdmin(String yyytId, Long deptId);

    /** 当前登录用户是否为其所在部门的部门管理员（创建权限校验用） */
    boolean currentIsDeptAdmin(String yyytId, Long deptId);

    /** 可用部门选项（dept_admin 已有部门去重） */
    List<Map<String, String>> deptOptions();
}

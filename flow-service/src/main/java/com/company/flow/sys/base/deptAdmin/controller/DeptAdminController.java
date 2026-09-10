package com.company.flow.sys.base.deptAdmin.controller;

import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.deptAdmin.entity.DeptAdmin;
import com.company.flow.sys.base.deptAdmin.service.DeptAdminService;
import com.company.flow.sys.base.result.Result;
import com.company.flow.sys.base.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门管理员接口（/dept-admin）
 */
@RestController
@RequestMapping("/dept-admin")
@CrossOrigin
public class DeptAdminController {

    @Autowired
    private DeptAdminService deptAdminService;

    /** 全部部门管理员 */
    @GetMapping("/list")
    public Result<List<DeptAdmin>> list() {
        return Result.success("获取成功", deptAdminService.listAll());
    }

    /** 新增部门管理员 */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody DeptAdmin admin) {
        return deptAdminService.add(admin) ? Result.success("添加成功") : Result.fail("添加失败（用户号+部门已存在或参数不完整）");
    }

    /** 更新部门管理员（body 传完整实体，按 adminYstId+deptId 联合主键更新） */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody DeptAdmin admin) {
        return deptAdminService.update(admin) ? Result.success("更新成功") : Result.fail("更新失败（用户号+部门不存在或参数不完整）");
    }

    /** 可用部门选项（按 dept_admin 表已有部门去重，新建管理员时部门下拉以此为准） */
    @GetMapping("/dept-options")
    public Result<List<Map<String, String>>> deptOptions() {
        return Result.success("获取成功", deptAdminService.deptOptions());
    }

    /** 删除部门管理员 body: { adminYstId, deptId } */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, String> body) {
        String adminYstId = body.get("adminYstId");
        String deptId = body.get("deptId");
        if (adminYstId == null || deptId == null) {
            return Result.fail("参数不完整");
        }
        return deptAdminService.delete(adminYstId, deptId) ? Result.success("删除成功") : Result.fail("删除失败");
    }

    /** 当前登录用户是否为部门管理员（超级管理员亦视为可审批；前端控制「待我审批」入口展示） */
    @GetMapping("/current-check")
    public Result<Boolean> currentCheck() {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) {
            return Result.success("获取成功", false);
        }
        if (Boolean.TRUE.equals(me.getSuperAdmin())) {
            return Result.success("获取成功", true);
        }
        return Result.success("获取成功", deptAdminService.deptIdOf(me.getYyytId()) != null);
    }
}

package com.zqk.house.sysuser.controller;

import com.zqk.house.sysuser.entity.SysUser;
import com.zqk.house.sysuser.entity.SysUserQueryForm;
import com.zqk.house.sysuser.service.SysUserService;
import com.zqk.house.sysuser.vo.LoginUser;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.Result;
import com.zqk.house.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sys-user")
@CrossOrigin
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/list")
    public Result<PageResult<SysUser>> list(@RequestBody SysUserQueryForm form) {
        PageResult<SysUser> pageResult = sysUserService.getPage(
                form.getPage(), form.getLimit(),
                form.getUsername(), form.getEmpNo(),
                form.getRealName(), form.getDeptName(),
                form.getStatus());
        return Result.success("获取成功", pageResult);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysUser user) {
        return sysUserService.addUser(user) ?
                Result.success("添加成功") : Result.fail("添加失败");
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysUser user) {
        if (user.getId() == null) {
            return Result.fail("ID不能为空");
        }
        return sysUserService.updateUser(user) ?
                Result.success("更新成功") : Result.fail("更新失败");
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return sysUserService.deleteUser(id) ?
                Result.success("删除成功") : Result.fail("删除失败");
    }

    /**
     * 演示：获取当前登录用户信息（通过 SecurityUtils 取 ThreadLocal）
     * 任意 Controller 方法都可用 SecurityUtils.getLoginUser() 拿到登录用户
     */
    @GetMapping("/current")
    public Result<LoginUser> current() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser != null ?
                Result.success("获取成功", loginUser) : Result.fail("未登录");
    }

    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        SysUser user = sysUserService.getUserById(id);
        return user != null ?
                Result.success("获取成功", user) : Result.notFound("用户不存在");
    }
}

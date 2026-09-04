package com.company.flow.sys.base.autuser.controller;

import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.entity.UserQueryForm;
import com.company.flow.sys.base.autuser.service.UserService;
import com.company.flow.sys.base.autuser.vo.SysUserStatsVO;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理接口（/sys-user，管理页：分页 / 统计 / 新增 / 更新 / 删除）
 */
@RestController
@RequestMapping("/sys-user")
@CrossOrigin
public class SysUserController {

    @Autowired
    private UserService userService;

    @PostMapping("/list")
    public Result<PageResult<User>> list(@RequestBody UserQueryForm form) {
        return Result.success("获取成功", userService.getPage(form));
    }

    /** 用户管理页统计卡 */
    @GetMapping("/stats")
    public Result<SysUserStatsVO> stats() {
        return Result.success("获取成功", userService.stats());
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody User user) {
        return userService.addUser(user) ?
                Result.success("添加成功") : Result.fail("添加失败");
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody User user) {
        if (!StringUtils.hasText(user.getYyytId())) {
            return Result.fail("用户号不能为空");
        }
        return userService.updateUser(user) ?
                Result.success("更新成功") : Result.fail("更新失败");
    }

    @DeleteMapping("/delete/{yyytId}")
    public Result<Void> delete(@PathVariable String yyytId) {
        return userService.deleteUser(yyytId) ?
                Result.success("删除成功") : Result.fail("删除失败");
    }

    @GetMapping("/{yyytId}")
    public Result<User> detail(@PathVariable String yyytId) {
        User user = userService.findByYyytId(yyytId);
        return user != null ?
                Result.success("获取成功", user) : Result.notFound("用户不存在");
    }
}

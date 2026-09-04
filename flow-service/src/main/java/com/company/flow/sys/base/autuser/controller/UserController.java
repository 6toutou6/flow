package com.company.flow.sys.base.autuser.controller;

import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.service.UserService;
import com.company.flow.sys.base.autuser.vo.LoginResponse;
import com.company.flow.sys.base.config.AdminProperties;
import com.company.flow.sys.base.config.AuthInterceptor;
import com.company.flow.sys.base.result.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 系统用户接口（autuser 模块）：登录 / 用户列表 / 模糊搜索
 * 登录成功后用户信息写入会话，后续接口无需再传用户信息（AuthInterceptor 自动写入上下文）
 */
@RestController
@RequestMapping("/autuser")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminProperties adminProperties;

    /**
     * 登录：按用户号（yyyt_id）优先、中文姓名兜底匹配；成功写入会话
     * 返回体只含 userName / yyytId / deptId / deptName / superAdmin，不含密码与自增主键
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody Map<String, String> params, HttpSession session) {
        String username = params.get("username");
        String password = params.get("password");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.fail("用户名或密码不能为空");
        }
        User user = userService.login(username, password);
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, user);
        LoginResponse resp = new LoginResponse();
        resp.setYyytId(user.getYyytId());
        resp.setUserName(user.getUserName());
        resp.setDeptId(user.getDeptId());
        resp.setDeptName(user.getDeptName());
        resp.setSuperAdmin(adminProperties.isSuperAdmin(user.getYyytId()));
        return Result.success("登录成功", resp);
    }

    /** 查询所有正常状态用户（POST /autuser/list；用于登录页下拉 / 选择处理人；密码脱敏） */
    @PostMapping("/list")
    public Result<List<User>> listAllActive() {
        List<User> list = userService.listAllActive();
        list.forEach(u -> u.setPassword(null));
        return Result.success("获取成功", list);
    }

    /**
     * 模糊搜索用户（POST /autuser/search；type 支持单个数字或数组 [0, 9]，未传不限制类型；密码脱敏）
     * 按用户号/中文姓名匹配，供 UserSelect 组件联想
     */
    @PostMapping("/search")
    public Result<List<User>> search(@RequestBody(required = false) Map<String, Object> params) {
        Object nameObj = params == null ? null : params.get("userName");
        String userName = nameObj == null ? null : String.valueOf(nameObj);
        List<User> list = userService.search(userName, typeList(params == null ? null : params.get("type")));
        list.forEach(u -> u.setPassword(null));
        return Result.success("获取成功", list);
    }

    /** 解析 type：兼容单个数字 / 字符串 / 数组（如 [0, 9]），非法值忽略 */
    private List<Integer> typeList(Object o) {
        if (o == null) {
            return null;
        }
        List<Integer> list = new ArrayList<>();
        if (o instanceof List<?> raw) {
            for (Object e : raw) {
                Integer v = intv(e);
                if (v != null) {
                    list.add(v);
                }
            }
        } else {
            Integer v = intv(o);
            if (v != null) {
                list.add(v);
            }
        }
        return list.isEmpty() ? null : list;
    }

    private Integer intv(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

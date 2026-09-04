package com.company.flow.sys.base.autuser.service;

import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.entity.UserQueryForm;
import com.company.flow.sys.base.autuser.vo.SysUserStatsVO;
import com.company.flow.sys.base.result.PageResult;

import java.util.List;

/**
 * 系统用户服务接口
 */
public interface UserService {

    /** 登录校验：按用户号 + 密码，成功返回用户（含密码），失败返回 null */
    User login(String yyytId, String password);

    /** 查询全部正常用户（登录页下拉选择，密码脱敏） */
    List<User> listAllActive();

    /** 模糊搜索用户（按用户号/中文姓名/邮箱），type 支持集合，未传不限制 */
    List<User> search(String userName, List<Integer> types);

    /** 管理页分页查询 */
    PageResult<User> getPage(UserQueryForm form);

    /** 用户管理页统计卡 */
    SysUserStatsVO stats();

    boolean addUser(User user);

    boolean updateUser(User user);

    boolean deleteUser(String yyytId);

    /** 按用户号查询（不含密码） */
    User findByYyytId(String yyytId);
}

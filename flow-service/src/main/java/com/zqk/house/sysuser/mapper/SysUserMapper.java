package com.zqk.house.sysuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.sysuser.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper（MyBatis-Plus BaseMapper）
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按用户名查询（含密码字段，用于登录校验）
     * 用原生 SQL 绕过实体的 @TableField(select=false)
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsernameWithPassword(@Param("username") String username);

    /** 部门数（有用户的部门） */
    @Select("SELECT COUNT(DISTINCT dept_id) FROM sys_user")
    Long countDistinctDept();
}

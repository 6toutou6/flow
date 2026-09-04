package com.company.flow.sys.base.autuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.flow.sys.base.autuser.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper（MyBatis-Plus BaseMapper）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按用户号查询（含密码字段，用于登录校验）
     * 用原生 SQL 绕过实体的 @TableField(select=false)
     */
    @Select("SELECT * FROM aut_user WHERE yyyt_id = #{yyytId}")
    User selectByYyytIdWithPassword(@Param("yyytId") String yyytId);

    /** 部门数（有用户的部门） */
    @Select("SELECT COUNT(DISTINCT dept_id) FROM aut_user")
    Long countDistinctDept();
}

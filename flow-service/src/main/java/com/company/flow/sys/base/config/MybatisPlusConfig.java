package com.company.flow.sys.base.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：Mapper 扫描 + 分页插件
 */
@Configuration
@MapperScan({"com.company.flow.sys.base.attach.mapper",
        "com.company.flow.sys.base.autuser.mapper",
        "com.company.flow.sys.base.deptAdmin.mapper",
        "com.company.flow.sys.flowdata.mapper",
        "com.company.flow.sys.flowtask.mapper",
        "com.company.flow.sys.flowtemplate.mapper"})
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}

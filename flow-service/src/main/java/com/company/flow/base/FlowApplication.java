package com.company.flow.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * flow 流程流转系统启动类（扫描根包 com.company.flow；Mapper 扫描见 MybatisPlusConfig）
 */
@SpringBootApplication(scanBasePackages = "com.company.flow")
@EnableScheduling
public class FlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowApplication.class, args);
    }

}

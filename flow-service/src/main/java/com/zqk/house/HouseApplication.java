package com.zqk.house;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"com.zqk.house.sysuser.mapper", "com.zqk.house.flowtemplate.mapper", "com.zqk.house.flowtask.mapper", "com.zqk.house.flowdata.mapper"})
public class HouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseApplication.class, args);
    }

}

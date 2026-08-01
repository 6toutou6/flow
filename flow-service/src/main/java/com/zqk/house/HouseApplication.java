package com.zqk.house;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.zqk.house.user.mapper", "com.zqk.house.room.mapper", "com.zqk.house.rentpayment.mapper", "com.zqk.house.sysuser.mapper"})
public class HouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseApplication.class, args);
    }

}

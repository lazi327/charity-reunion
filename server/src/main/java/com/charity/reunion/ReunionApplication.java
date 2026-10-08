package com.charity.reunion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 公益寻亲平台 后端启动类
 */
@SpringBootApplication
@MapperScan("com.charity.reunion.mapper")
public class ReunionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReunionApplication.class, args);
        System.out.println("公益寻亲平台后端已启动: http://localhost:8081");
    }
}

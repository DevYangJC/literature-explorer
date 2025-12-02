package com.yuyuan.literature;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yuyuan.literature.mapper")
public class LiteratureAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteratureAssistantApplication.class, args);
    }

}

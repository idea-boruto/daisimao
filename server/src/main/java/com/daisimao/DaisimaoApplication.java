package com.daisimao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.daisimao.repository")
public class DaisimaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaisimaoApplication.class, args);
    }
}

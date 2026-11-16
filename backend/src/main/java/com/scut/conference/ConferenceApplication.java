package com.scut.conference;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 会议管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.scut.conference.mapper")
public class ConferenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConferenceApplication.class, args);
    }
}

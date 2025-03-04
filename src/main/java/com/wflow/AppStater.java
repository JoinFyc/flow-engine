package com.wflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @author : JoinFyc
 * @date : 2024/6/27
 */
@SpringBootApplication
@MapperScan(basePackages = {"com.wflow.mapper"})
public class AppStater {

    public static void main(String[] args) {
        SpringApplication.run(AppStater.class, args);
    }
}

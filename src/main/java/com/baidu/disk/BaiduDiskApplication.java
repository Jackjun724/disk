package com.baidu.disk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class BaiduDiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaiduDiskApplication.class, args);
    }
}

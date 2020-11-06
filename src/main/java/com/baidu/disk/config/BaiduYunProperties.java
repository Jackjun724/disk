package com.baidu.disk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author JackJun
 * @date 2020/11/5 5:38 下午
 */
@Data
@Component
@ConfigurationProperties("baidu")
public class BaiduYunProperties {
    private Long uid;
    private String bduss;
    private String id;
}

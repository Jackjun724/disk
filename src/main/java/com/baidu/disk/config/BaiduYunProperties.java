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
    private String uid;
    private String bduss;
    private String stoken;
    private String bdstoken;
    private String soPath;
    private String apkPath;
    private String devUid;
    private String cuid;
}

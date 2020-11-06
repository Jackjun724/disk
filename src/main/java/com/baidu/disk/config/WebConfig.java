package com.baidu.disk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author JackJun
 * @date 2020/11/6 6:00 下午
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private WebInterceptor webInterceptor = new WebInterceptor();

    /**
     * 注册 拦截器
     */
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor);
    }
}

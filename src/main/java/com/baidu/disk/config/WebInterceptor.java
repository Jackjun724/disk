package com.baidu.disk.config;

import com.baidu.disk.web.base.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.forezp.distributedlimitcore.constant.LimitType;
import io.github.forezp.distributedlimitcore.entity.LimitEntity;
import io.github.forezp.distributedlimitcore.entity.LimitResult;
import io.github.forezp.distributedlimitcore.limit.GuavaLimitExcutor;
import io.github.forezp.distributedlimitcore.limit.LimitExcutor;
import io.github.forezp.distributedlimitcore.util.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author JackJun
 * @date 2020/11/6 5:52 下午
 */
@Slf4j
public class WebInterceptor extends HandlerInterceptorAdapter {

    private ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, LimitEntity> limitEntityMap = new ConcurrentHashMap<>();

    private LimitExcutor limitExcutor = new GuavaLimitExcutor();

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //限流2个维度
        //IP
        String identifier = getClientIp(request);
        //api维度
        String key = request.getRequestURI();
        String composeKey = KeyUtil.compositeKey(identifier, key);
        LimitEntity limitEntity = limitEntityMap.get(composeKey);
        if (limitEntity == null) {
            limitEntity = new LimitEntity();
            limitEntity.setIdentifier(identifier);
            limitEntity.setKey(key);
            limitEntity.setSeconds(2);
            limitEntity.setLimtNum(1);
            limitEntity.setLimitType(LimitType.USER_URL);
            limitEntityMap.putIfAbsent(composeKey, limitEntity);
        }

        if (limitExcutor.tryAccess(limitEntity).getResultType() != LimitResult.ResultType.SUCCESS) {
            returnJson(response);
            return false;
        }

        return true;
    }

    private void returnJson(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(objectMapper.writeValueAsString(BaseResponse.failure("请求过快，等待5秒后重试！")));
        } catch (IOException e) {
            log.error("Error", e);
        }
    }

    /**
     * 获取客户端ip地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.trim().equals("") || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.trim().equals("") || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.trim().equals("") || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个路由时，取第一个非unknown的ip
        final String[] arr = ip.split(",");
        for (final String str : arr) {
            if (!"unknown".equalsIgnoreCase(str)) {
                ip = str;
                break;
            }
        }
        return ip;
    }
}

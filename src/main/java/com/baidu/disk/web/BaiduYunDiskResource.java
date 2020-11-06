package com.baidu.disk.web;

import com.baidu.disk.requester.LinkHelper;
import com.baidu.disk.web.base.BaseResponse;
import com.baidu.disk.web.exception.ExpireException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author JackJun
 * @date 2020/11/6 2:30 下午
 */
@Slf4j
@RestController
@AllArgsConstructor
public class BaiduYunDiskResource {

    private final LinkHelper helper;

    @GetMapping("/api/download/link")
    @SuppressWarnings("unchecked")
    public BaseResponse<?> getDownloadLink(@RequestParam("fsId") String fsId,
                                           @RequestParam("timestamp") String timestamp,
                                           @RequestParam("sign") String sign,
                                           @RequestParam("randsk") String randsk,
                                           @RequestParam("shareId") String shareId,
                                           @RequestParam("uk") String uk) {
        try {
            Map<String, Object> res = (Map<String, Object>) helper.getDLink(fsId, timestamp, sign, randsk, shareId, uk);
            if (res.get("error_code") == null) {
                List<Map<String, Object>> url = (List<Map<String, Object>>) res.get("urls");
                return BaseResponse.success(((String)url.get(0).get("url")).replace("http://","https://"));
            }
        } catch (ExpireException e) {
            return BaseResponse.failure("参数过期！");
        } catch (Exception e) {
            log.error("请求异常", e);
        }
        return BaseResponse.failure("获取下载链接错误！");
    }
}

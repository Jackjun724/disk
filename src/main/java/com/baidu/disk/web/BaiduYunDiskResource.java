package com.baidu.disk.web;

import com.baidu.disk.algorithm.SoSign;
import com.baidu.disk.requester.LinkHelper;
import com.baidu.disk.web.base.BaseResponse;
import com.baidu.disk.web.exception.ExpireException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author JackJun
 * @date 2020/11/6 2:30 下午
 */
@Slf4j
@RestController
@AllArgsConstructor
public class BaiduYunDiskResource {

    private final LinkHelper helper;

    private final SoSign soSign;

    @GetMapping("/api/download/link")
    public BaseResponse<?> getDownloadLink(@RequestParam("fsId") String fsId,
                                           @RequestParam("timestamp") String timestamp,
                                           @RequestParam("sign") String sign,
                                           @RequestParam("randsk") String randsk,
                                           @RequestParam("shareId") String shareId,
                                           @RequestParam("uk") String uk) {
        try {
            return BaseResponse.success(helper.getDLink(fsId, timestamp, sign, randsk, shareId, uk));
        } catch (ExpireException e) {
            return BaseResponse.failure("参数过期！");
        } catch (Exception e) {
            log.error("请求异常", e);
        }
        return BaseResponse.failure("获取下载链接错误！");
    }

    @GetMapping("/api/download/generate")
    public BaseResponse<?> getDownloadUrl(@RequestParam("url") String url,
                                          @RequestParam("bduss") String bduss,
                                          @RequestParam("uid") String uid,
                                          @RequestParam("uinfo") String uinfo) {
        try {
            return BaseResponse.success(soSign.handlerUrl(url, uinfo,bduss, uid));
        } catch (ExpireException e) {
            return BaseResponse.failure("参数过期！");
        } catch (Exception e) {
            log.error("请求异常", e);
        }
        return BaseResponse.failure("获取下载链接错误！");
    }
}

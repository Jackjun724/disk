package com.baidu.disk.web;

import com.baidu.disk.requester.LinkHelper;
import com.baidu.disk.web.base.BaseResponse;
import com.baidu.disk.web.exception.ExpireException;
import com.baidu.disk.web.exception.ServiceException;
import com.baidu.disk.web.vo.DownloadUrl;
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

    @GetMapping("/api/download/link")
    public BaseResponse<?> getDownloadLink(@RequestParam("shareId") String shareId,
                                           @RequestParam("uk") String uk,
                                           @RequestParam("pwd") String pwd,
                                           @RequestParam("path") String path,
                                           @RequestParam(value = "codeStr", required = false) String codeStr,
                                           @RequestParam(value = "code", required = false) String code) {
        try {
            DownloadUrl downloadUrl = helper.getDLink(shareId, uk, pwd, path, codeStr, code);
            if (downloadUrl.getCodeStr() == null) {
                return BaseResponse.success(downloadUrl);
            } else {
                return BaseResponse.failure("请输入验证码", downloadUrl, -10);
            }
        } catch (ServiceException e) {
            return BaseResponse.failure(e.getMsg());
        } catch (ExpireException e) {
            return BaseResponse.failure("参数过期！");
        } catch (Exception e) {
            log.error("请求异常", e);
        }
        return BaseResponse.failure("获取下载链接错误！");
    }

    @GetMapping("/api/captcha")
    public BaseResponse<?> getCaptcha() {
        try {
            BaseResponse.success(helper.getCaptcha());
        } catch (Exception e) {
            log.error("请求异常", e);
        }
        return BaseResponse.failure("获取验证码失败！");
    }
}

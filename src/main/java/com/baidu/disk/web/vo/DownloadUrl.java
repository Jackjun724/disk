package com.baidu.disk.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author JackJun
 * @date 2020/11/16 下午4:27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownloadUrl {
    private String url;
    private String ua;
    private String codeStr;
    private String captchaUrl;
}

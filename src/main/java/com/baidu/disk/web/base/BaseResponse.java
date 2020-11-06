package com.baidu.disk.web.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author JackJun
 * @date 2020/11/6 2:36 下午
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private int code;
    private String message;
    private T data;


    public static BaseResponse<?> success() {
        return BaseResponse.builder()
                .message("OK")
                .code(0)
                .build();
    }

    public static <T> BaseResponse<?> success(T data) {
        return BaseResponse.builder()
                .message("OK")
                .code(0)
                .data(data).build();
    }

    public static BaseResponse<?> failure(String message) {
        return BaseResponse.builder()
                .message(message)
                .code(-1)
                .build();
    }
}

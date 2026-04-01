package com.syt.graduationproject.model.response;

import com.syt.graduationproject.enums.ResponseCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> Response<T> success() {
        return new Response<>(ResponseCodeEnum.SUCCESS.getCode(),
                ResponseCodeEnum.SUCCESS.getMessage(),
                null);
    }
    
    public static <T> Response<T> success(T data) {
        return new Response<>(ResponseCodeEnum.SUCCESS.getCode(),
                ResponseCodeEnum.SUCCESS.getMessage(),
                data);
    }

    public static <T> Response<T> success(String message, T data) {
        return new Response<>(ResponseCodeEnum.SUCCESS.getCode(),
                message,
                data);
    }

    public static <T> Response<T> fail() {
        return new Response<>(ResponseCodeEnum.FAIL.getCode(),
                ResponseCodeEnum.FAIL.getMessage(),
                null);
    }

    public static <T> Response<T> fail(String message) {
        return new Response<>(ResponseCodeEnum.FAIL.getCode(),
                message,
                null);
    }

    public static <T> Response<T> fail(String message, T data) {
        return new Response<>(ResponseCodeEnum.FAIL.getCode(),
                message,
                data);
    }
}

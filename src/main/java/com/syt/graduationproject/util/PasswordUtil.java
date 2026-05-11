package com.syt.graduationproject.util;

import com.syt.graduationproject.exception.ErrorParamException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PasswordUtil {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{6,16}$");

    public static String md5(String plaintext) {
        return DigestUtils.md5DigestAsHex(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public static void validate(String plaintext) {
        if (plaintext == null || !PASSWORD_PATTERN.matcher(plaintext).matches()) {
            throw new ErrorParamException("密码必须为6-16位大小写字母与数字的组合");
        }
    }
}

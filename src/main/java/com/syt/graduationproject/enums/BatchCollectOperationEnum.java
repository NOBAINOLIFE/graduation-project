package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BatchCollectOperationEnum {

    /**
     * 清除失效内容
     */
    CLEAR_INVALID_CONTENT(0, "清除失效内容"),

    /**
     * 批量取消收藏
     */
    CANCEL_COLLECT(1, "批量取消收藏"),

    /**
     * 复制
     */
    COPY(2, "复制"),

    /**
     * 移动
     */
    MOVE(3, "移动");

    private final Integer code;

    private final String message;

    public static BatchCollectOperationEnum fromCode(Integer code) {
        for (BatchCollectOperationEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}

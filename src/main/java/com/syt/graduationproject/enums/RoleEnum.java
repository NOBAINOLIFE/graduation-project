package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {

    ADMIN(0L, "ADMIN", "管理员"),

    USER(1L, "USER", "普通用户");

    private final Long roleId;

    private final String roleCode;

    private final String roleName;

    public static RoleEnum fromRoleId(Long roleId) {
        if (roleId == null) {
            return null;
        }
        for (RoleEnum value : values()) {
            if (value.getRoleId().equals(roleId)) {
                return value;
            }
        }
        return null;
    }
}

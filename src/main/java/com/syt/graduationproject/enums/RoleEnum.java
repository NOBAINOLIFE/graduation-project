package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {

    USER(1L, "USER", "普通用户"),

    ADMIN(2L, "ADMIN", "管理员");

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

    public static RoleEnum fromRoleCode(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        for (RoleEnum value : values()) {
            if (value.getRoleCode().equalsIgnoreCase(roleCode)) {
                return value;
            }
        }
        return null;
    }
}


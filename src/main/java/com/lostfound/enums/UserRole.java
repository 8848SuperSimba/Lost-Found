package com.lostfound.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole implements IEnum<String> {
    USER("USER"),
    ADMIN("ADMIN"),
    SUPER_ADMIN("SUPER_ADMIN");

    @EnumValue
    private final String value;

    @Override
    public String getValue() {
        return value;
    }

    public boolean canAccessAdminPanel() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean canGrantAdminRole() {
        return this == SUPER_ADMIN;
    }
}

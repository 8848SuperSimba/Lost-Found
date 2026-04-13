package com.lostfound.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole implements IEnum<String> {
    USER("USER"),
    ADMIN("ADMIN");

    @EnumValue
    private final String value;

    @Override
    public String getValue() {
        return value;
    }
}

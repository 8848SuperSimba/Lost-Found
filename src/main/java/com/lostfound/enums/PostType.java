package com.lostfound.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostType implements IEnum<String> {
    LOST("LOST", "失物"),
    FOUND("FOUND", "寻物");

    @EnumValue
    private final String value;

    private final String desc;

    @Override
    public String getValue() {
        return value;
    }
}

package com.lostfound.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostStatus implements IEnum<String> {
    OPEN("OPEN", "进行中"),
    MATCHED("MATCHED", "已匹配"),
    RESOLVED("RESOLVED", "已找回"),
    CLOSED("CLOSED", "已关闭");

    @EnumValue
    private final String value;

    private final String desc;

    @Override
    public String getValue() {
        return value;
    }
}

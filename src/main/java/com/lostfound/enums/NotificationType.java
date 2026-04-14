package com.lostfound.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType implements IEnum<String> {
    MATCH("MATCH", "匹配通知"),
    MESSAGE("MESSAGE", "消息通知"),
    SYSTEM("SYSTEM", "系统通知");

    @EnumValue
    private final String value;

    private final String desc;

    @Override
    public String getValue() {
        return value;
    }
}

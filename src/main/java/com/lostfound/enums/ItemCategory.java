package com.lostfound.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemCategory implements IEnum<String> {
    CERTIFICATE("CERTIFICATE", "证件"),
    ELECTRONICS("ELECTRONICS", "数码"),
    KEY("KEY", "钥匙"),
    CLOTHING("CLOTHING", "衣物"),
    BOOK("BOOK", "书籍"),
    OTHER("OTHER", "其他");

    @EnumValue
    private final String value;

    private final String desc;

    @Override
    public String getValue() {
        return value;
    }
}

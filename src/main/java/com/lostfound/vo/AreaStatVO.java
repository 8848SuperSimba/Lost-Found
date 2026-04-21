package com.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaStatVO {

    private String areaCode;
    private String areaText;
    private Long count;
}

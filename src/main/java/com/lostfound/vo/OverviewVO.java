package com.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewVO {

    private Long lostCount;
    private Long foundCount;
    private Long resolvedCount;
    private String resolvedRate;
    private Long todayCount;
    private Long openCount;
}

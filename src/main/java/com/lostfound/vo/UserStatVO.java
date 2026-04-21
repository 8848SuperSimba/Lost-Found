package com.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatVO {

    private Long totalCount;
    private Long todayCount;
    private Long bannedCount;
    private Long activeCount;
}

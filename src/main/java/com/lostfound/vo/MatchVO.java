package com.lostfound.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchVO {

    private Long matchId;
    private BigDecimal score;
    private Integer scorePercent;
    private Double categoryScore;
    private Double keywordScore;
    private Double areaScore;
    private Double timeScore;
    private List<String> matchReasons;
    private PostVO post;
}

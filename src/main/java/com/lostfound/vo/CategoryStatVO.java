package com.lostfound.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatVO {

    private String categoryValue;
    private String categoryDesc;
    private Long count;
    private String percentage;
}

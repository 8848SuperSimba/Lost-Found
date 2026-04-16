package com.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateThreadRequest {

    @NotNull(message = "失物帖ID不能为空")
    private Long lostPostId;

    @NotNull(message = "寻物帖ID不能为空")
    private Long foundPostId;
}

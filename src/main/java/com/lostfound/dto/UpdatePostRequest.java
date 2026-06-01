package com.lostfound.dto;

import com.lostfound.enums.ItemCategory;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UpdatePostRequest {

    @Size(max = 30, message = "标题长度不能超过30")
    private String title;

    private ItemCategory category;

    @Size(max = 300, message = "描述长度不能超过300")
    private String description;

    @PastOrPresent(message = "遗失/拾获时间不能是未来时间")
    private LocalDateTime lostFoundTime;

    private String areaCode;
    private String areaText;
    private String locationText;
    private String contactInfo;
    private String reward;
    private List<String> keywords;
    private List<String> imageUrls;
}

package com.lostfound.dto;

import com.lostfound.enums.ItemCategory;
import com.lostfound.enums.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CreatePostRequest {

    @NotNull(message = "帖子类型不能为空")
    private PostType postType;

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题长度不能超过128")
    private String title;

    @NotNull(message = "分类不能为空")
    private ItemCategory category;

    private String description;

    @NotNull(message = "遗失/拾获时间不能为空")
    @PastOrPresent(message = "遗失/拾获时间不能是未来时间")
    private LocalDateTime lostFoundTime;

    @NotBlank(message = "区域编码不能为空")
    private String areaCode;

    private String areaText;
    private String locationText;
    private String contactInfo;
    private String reward;
    private List<String> keywords;
    private List<String> imageUrls;
}

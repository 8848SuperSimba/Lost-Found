package com.lostfound.vo;

import com.lostfound.enums.ItemCategory;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostVO {

    private Long id;
    private PostType postType;
    private String title;
    private ItemCategory category;
    private String areaText;
    private LocalDateTime lostFoundTime;
    private LocalDateTime createdAt;
    private PostStatus status;
    private String publisherNickname;
    private String publisherAvatar;
    private String coverImageUrl;
    private String descriptionSummary;

    private String description;
    private String locationText;
    private String contactInfo;
    private String reward;
    private List<String> keywords;
    private List<String> imageUrls;
    private Long publisherUserId;
}

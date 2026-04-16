package com.lostfound.vo;

import com.lostfound.enums.ThreadStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThreadVO {

    private Long id;
    private Long lostPostId;
    private Long foundPostId;
    private Long otherUserId;
    private String otherNickname;
    private String otherAvatar;
    private String relatedPostTitle;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
    private ThreadStatus status;
    private String otherContactInfo;
    private String myContactInfo;
}

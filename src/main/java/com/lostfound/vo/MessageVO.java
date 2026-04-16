package com.lostfound.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageVO {

    private Long id;
    private Long threadId;
    private Long senderUserId;
    private String senderNickname;
    private String senderAvatar;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Boolean isSelf;
}

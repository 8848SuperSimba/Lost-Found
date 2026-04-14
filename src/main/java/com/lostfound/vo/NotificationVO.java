package com.lostfound.vo;

import com.lostfound.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationVO {

    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private String refType;
    private Long refId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

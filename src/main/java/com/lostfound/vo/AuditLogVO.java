package com.lostfound.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogVO {

    private Long id;
    private Long adminUserId;
    private String adminUsername;
    private String adminNickname;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private LocalDateTime createdAt;
}

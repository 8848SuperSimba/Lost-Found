package com.lostfound.vo;

import com.lostfound.enums.UserRole;
import com.lostfound.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String phone;
    private String email;
    private String nickname;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}

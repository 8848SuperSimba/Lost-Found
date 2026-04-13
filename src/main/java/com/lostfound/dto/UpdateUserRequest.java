package com.lostfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;

    @Email(message = "邮箱格式错误")
    private String email;

    @Size(max = 512, message = "头像地址长度不能超过512")
    private String avatarUrl;
}

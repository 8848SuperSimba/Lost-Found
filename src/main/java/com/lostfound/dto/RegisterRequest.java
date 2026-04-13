package com.lostfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在3到20之间")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "用户名只能包含字母和数字")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
    private String phone;

    @Email(message = "邮箱格式错误")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}

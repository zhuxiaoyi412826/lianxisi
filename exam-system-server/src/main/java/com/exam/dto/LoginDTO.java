package com.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @Schema(description = "登录类型：EMAIL邮箱/ACCOUNT账号", example = "EMAIL")
    private String loginType;

    @Schema(description = "邮箱地址（邮箱登录时必填）", example = "2927883590@qq.com")
    private String email;

    @Schema(description = "验证码（邮箱登录时必填，6位数字）", example = "123456")
    private String code;

    @Schema(description = "用户名（账号登录时必填）", example = "zhangsan")
    private String username;

    @Schema(description = "密码（账号登录时必填）", example = "123456")
    private String password;
}

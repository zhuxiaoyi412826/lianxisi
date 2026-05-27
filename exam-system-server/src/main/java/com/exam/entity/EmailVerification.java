package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邮箱验证码实体类
 */
@Data
@TableName("email_verification")
@Schema(description = "邮箱验证码")
public class EmailVerification {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "邮箱地址")
    private String email;

    @Schema(description = "验证码（6位数字）")
    private String code;

    @Schema(description = "验证码类型：LOGIN登录/REGISTER注册/BIND绑定/CHANGE_PWD修改密码")
    private String type;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否已使用：0未使用/1已使用")
    private Boolean used;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

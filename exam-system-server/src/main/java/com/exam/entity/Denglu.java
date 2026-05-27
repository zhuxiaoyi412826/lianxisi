package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户登录信息实体类 - 独立于原users表的登录账号信息
 */
@Data
@TableName("denglu")
@Schema(description = "用户登录信息")
@JsonIgnoreProperties({"handler", "fields"})
public class Denglu {

    @Schema(description = "用户ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "邮箱账号")
    private String email;

    @Schema(description = "学号（6位数字）")
    private String studentNo;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别：male/female/other")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthday;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "状态：ACTIVE活跃/INACTIVE未激活/BANNED封禁")
    private String status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "登录方式：EMAIL邮箱/ACCOUNT账号")
    private String loginType;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

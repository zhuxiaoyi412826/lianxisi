package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.LoginDTO;
import com.exam.entity.Denglu;
import com.exam.service.DengluService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "用户认证", description = "用户登录、注册、验证码发送等功能")
public class AuthController {

    @Autowired
    private DengluService dengluService;

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送6位验证码")
    public Result<Void> sendCode(
            @Parameter(description = "邮箱地址") @RequestParam String email,
            @Parameter(description = "类型：LOGIN登录/REGISTER注册") @RequestParam(defaultValue = "LOGIN") String type) {

        boolean success = dengluService.sendEmailCode(email, type); //  调用服务层方法发送邮箱验证码
        if (success) {
            return Result.success(null, "验证码已发送");
        } else {
            return Result.error("验证码发送失败");
        }
    }

    /**
     * 邮箱验证码登录/注册
     */
    @PostMapping("/login-email")
    @Operation(summary = "邮箱验证码登录", description = "使用邮箱和验证码登录，自动注册新账号")
    public Result<Denglu> loginWithEmail(
            @RequestBody LoginDTO loginDTO,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        try {
            Denglu user = dengluService.loginWithEmail(
                loginDTO.getEmail(),
                loginDTO.getCode(),
                ip
            );
            return Result.success(user, "登录成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 账号密码登录
     */
    @PostMapping("/login-account")
    @Operation(summary = "账号密码登录", description = "使用用户名和密码登录")
    public Result<Denglu> loginWithAccount(
            @RequestBody LoginDTO loginDTO,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        try {
            Denglu user = dengluService.loginWithAccount(
                loginDTO.getUsername(),
                loginDTO.getPassword(),
                ip
            );
            return Result.success(user, "登录成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的信息")
    public Result<Denglu> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return Result.error("请先登录");
        }
        Denglu user = dengluService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 隐藏敏感信息
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update-profile")
    @Operation(summary = "更新用户信息", description = "更新个人资料")
    public Result<Void> updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Denglu user) {

        if (userId == null) {
            return Result.error("请先登录");
        }
        user.setId(userId);
        user.setPassword(null); // 不允许通过此接口修改密码
        boolean success = dengluService.updateById(user);
        return success ? Result.success(null, "更新成功") : Result.error("更新失败");
    }

    /**
     * 修改密码
     */
    @PutMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改账号密码")
    public Result<Void> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        if (userId == null) {
            return Result.error("请先登录");
        }
        Denglu user = dengluService.getById(userId);
        if (user == null || !oldPassword.equals(user.getPassword())) {
            return Result.error("原密码错误");
        }
        user.setPassword(newPassword);
        boolean success = dengluService.updateById(user);
        return success ? Result.success(null, "密码修改成功") : Result.error("密码修改失败");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

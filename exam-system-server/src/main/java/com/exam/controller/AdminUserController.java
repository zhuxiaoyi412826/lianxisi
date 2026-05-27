package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.Result;
import com.exam.entity.Denglu;
import com.exam.service.DengluService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "后台用户管理", description = "管理员用户管理功能")
public class AdminUserController {

    @Autowired
    private DengluService dengluService;

    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    public Result<Page<Denglu>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {

        Page<Denglu> pageInfo = new Page<>(page, size);
        QueryWrapper<Denglu> wrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("username", keyword)
                          .or().like("email", keyword)
                          .or().like("real_name", keyword)
                          .or().like("student_no", keyword));
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("create_time");
        Page<Denglu> result = dengluService.page(pageInfo, wrapper);

        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));

        return Result.success(result);
    }

    /**
     * 获取所有用户（下拉框用）
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有用户", description = "获取所有用户列表")
    public Result<List<Denglu>> getAllUsers() {
        List<Denglu> users = dengluService.list();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public Result<Denglu> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id) {

        Denglu user = dengluService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "启用/禁用/封禁用户")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {

        boolean success = dengluService.updateStatus(id, status);
        return success ? Result.success(null, "状态更新成功") : Result.error("状态更新失败");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {

        boolean success = dengluService.removeById(id);
        return success ? Result.success(null, "删除成功") : Result.error("删除失败");
    }
}

package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.entity.Denglu;
import com.exam.entity.EmailVerification;
import com.exam.mapper.DengluMapper;
import com.exam.mapper.EmailVerificationMapper;
import com.exam.service.DengluService;
import com.exam.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 用户登录Service实现
 */
@Slf4j
@Service
public class DengluServiceImpl extends ServiceImpl<DengluMapper, Denglu> implements DengluService {

    @Autowired
    private EmailVerificationMapper verificationMapper;

    @Autowired
    private EmailService emailService;

    private final Random random = new Random();

    @Override
    @Transactional
    public Denglu loginWithEmail(String email, String code, String ip) {
        // 验证验证码
        if (!verifyCode(email, code, "LOGIN")) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 标记验证码已使用
        EmailVerification verification = verificationMapper.findValidCode(email, code, "LOGIN");
        if (verification != null) {
            verification.setUsed(true);
            verificationMapper.updateById(verification);
        }

        // 查询用户是否存在
        QueryWrapper<Denglu> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        Denglu user = this.getOne(wrapper);

        // 如果用户不存在，自动注册
        if (user == null) {
            user = new Denglu();
            user.setEmail(email);
            user.setStudentNo(generateNextStudentNo());
            user.setUsername("user" + user.getStudentNo());
            user.setStatus("ACTIVE");
            user.setLoginType("EMAIL");
            this.save(user);
            log.info("邮箱登录自动注册新用户: {}", email);
        }

        // 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        this.updateById(user);

        return user;
    }

    @Override
    public Denglu loginWithAccount(String username, String password, String ip) {
        QueryWrapper<Denglu> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username)
               .eq("password", password)
               .eq("status", "ACTIVE");
        Denglu user = this.getOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        this.updateById(user);

        return user;
    }

    @Override
    @Transactional
    public boolean sendEmailCode(String email, String type) {
        try {
            // 生成6位验证码
            String code = String.format("%06d", random.nextInt(1000000));

            // 保存验证码
            EmailVerification verification = new EmailVerification();
            verification.setEmail(email);
            verification.setCode(code);
            verification.setType(type);
            verification.setExpireTime(LocalDateTime.now().plusMinutes(10));
            verification.setUsed(false);
            verificationMapper.insert(verification);
            log.info("验证码已保存到数据库: email={}, code={}", email, code);

            // 发送邮件
            emailService.sendVerificationCode(email, code);
            log.info("验证码发送成功: {}, code: {}", email, code);
            return true;
        } catch (Exception e) {
            log.error("验证码发送失败: email={}, error={}", email, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean verifyCode(String email, String code, String type) {
        EmailVerification verification = verificationMapper.findValidCode(email, code, type);
        return verification != null;
    }

    @Override
    public String generateNextStudentNo() {
        // 获取最大学号
        QueryWrapper<Denglu> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("student_no").last("LIMIT 1");
        Denglu lastUser = this.getOne(wrapper);

        int nextNo = 1;
        if (lastUser != null && lastUser.getStudentNo() != null) {
            try {
                nextNo = Integer.parseInt(lastUser.getStudentNo()) + 1;
            } catch (NumberFormatException e) {
                nextNo = 1;
            }
        }

        return String.format("%06d", nextNo);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        Denglu user = this.getById(id);
        if (user == null) {
            return false;
        }
        user.setStatus(status);
        return this.updateById(user);
    }
}

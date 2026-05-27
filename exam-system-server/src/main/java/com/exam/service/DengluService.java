package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.Denglu;
import com.exam.entity.EmailVerification;
import java.util.List;

/**
 * 用户登录Service接口
 */
public interface DengluService extends IService<Denglu> {

    /**
     * 邮箱验证码登录/注册
     * @param email 邮箱
     * @param code 验证码
     * @param ip 登录IP
     * @return 用户信息
     */
    Denglu loginWithEmail(String email, String code, String ip);

    /**
     * 账号密码登录
     * @param username 用户名
     * @param password 密码
     * @param ip 登录IP
     * @return 用户信息
     */
    Denglu loginWithAccount(String username, String password, String ip);

    /**
     * 发送邮箱验证码
     * @param email 邮箱
     * @param type 类型：LOGIN登录/REGISTER注册
     * @return 是否成功
     */
    boolean sendEmailCode(String email, String type);

    /**
     * 验证邮箱验证码
     * @param email 邮箱
     * @param code 验证码
     * @param type 类型
     * @return 是否有效
     */
    boolean verifyCode(String email, String code, String type);

    /**
     * 生成下一个学号
     */
    String generateNextStudentNo();

    /**
     * 更新用户状态
     */
    boolean updateStatus(Long id, String status);
}

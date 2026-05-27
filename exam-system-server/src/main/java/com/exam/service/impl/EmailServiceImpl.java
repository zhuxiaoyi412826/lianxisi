package com.exam.service.impl;

import com.exam.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service /** * 邮件服务实现类 * 实现EmailService接口，提供邮件发送功能 */
public class EmailServiceImpl implements EmailService {

    @Autowired /**     * JavaMailSender对象，用于发送邮件     * 通过Spring自动注入     */
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:2927883590@qq.com}")
    private String fromEmail;

    @Value("${email.from-name:智能学习平台}") /**     * 发件人名称     * 从配置文件中获取，默认值为"智能学习平台"     */
    private String fromName;

    @Override
    public void sendVerificationCode(String to, String code) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); 
        //  创建MimeMessageHelper辅助类，用于设置邮件的各种属性，参数分别为：邮件消息对象、是否支持多部分消息、字符编码

        helper.setFrom(fromEmail, fromName); //  设置发件人信息，参数分别为：发件人邮箱、发件人名称
        helper.setTo(to); //  设置收件人邮箱
        helper.setSubject("【智能学习平台】您的验证码"); //  设置邮件主题

        String htmlContent = buildEmailContent(code); //  构建邮件的HTML内容，参数为验证码
        helper.setText(htmlContent, true); //  设置邮件内容，参数分别为：邮件内容、是否为HTML格式

        mailSender.send(message); //  使用邮件发送器发送邮件消息
        log.info("验证码邮件发送成功: {}, code: {}", to, code);
    }

    private String buildEmailContent(String code) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" + //  返回一个完整的HTML邮件内容，包含样式和布局
               "  <div style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;\">" +
               "    <h1 style=\"color: white; margin: 0;\">智能学习平台</h1>" + //  邮件头部标题
               "  </div>" +
               "  <div style=\"padding: 40px; background: #ffffff;\">" +
               "    <h2 style=\"color: #333;\">您好！</h2>" +
               "    <p style=\"color: #666; font-size: 16px;\">您正在进行 " +
               (code.length() == 6 ? "邮箱验证" : "身份验证") + //  根据验证码长度判断是邮箱验证还是身份验证
               "，您的验证码是：</p>" +
               "    <div style=\"background: #f5f5f5; padding: 20px; text-align: center; margin: 30px 0; border-radius: 8px;\">" +
               "      <span style=\"font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 8px;\">" + code + "</span>" +
               "    </div>" +
               "    <p style=\"color: #999; font-size: 14px;\">验证码10分钟内有效，请勿泄漏给他人。</p>" + //  有效期提示
               "    <hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\">" + //  分割线
               "    <p style=\"color: #999; font-size: 12px;\">如果您没有进行任何操作，请忽略此邮件。</p>" +
               "  </div>" +
               "</div>";
    }
}

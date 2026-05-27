package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.EmailVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 邮箱验证码Mapper
 */
@Mapper
public interface EmailVerificationMapper extends BaseMapper<EmailVerification> {

    @Select("SELECT * FROM email_verification " +
            "WHERE email = #{email} AND code = #{code} AND type = #{type} " +
            "AND used = 0 AND expire_time > NOW() " +
            "ORDER BY create_time DESC LIMIT 1")
    EmailVerification findValidCode(String email, String code, String type);
}

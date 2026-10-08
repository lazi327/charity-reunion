package com.charity.reunion.dto;

import lombok.Data;

/** 登录请求 */
@Data
public class LoginDTO {
    private String username;
    private String password;
    /** 图形验证码 id(与图片一对一对应,一次性消费) */
    private String captchaId;
    /** 算式答案 */
    private String captchaCode;
}

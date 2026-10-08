package com.charity.reunion.dto;

import lombok.Data;

/** 找回密码请求:凭注册时填写的手机号核验身份 */
@Data
public class ForgotPasswordDTO {
    private String username;
    private String phone;
    private String captchaId;
    private String captchaCode;
}

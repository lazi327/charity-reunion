package com.charity.reunion.dto;

import lombok.Data;

/** 注册请求 */
@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private Integer gender;      // 1男 2女
    private Integer birthYear;
    private String city;
    private String aboutMe;
    private String phone;
}

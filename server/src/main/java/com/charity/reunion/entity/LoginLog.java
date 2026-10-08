package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 登录安全日志:记录每一次登录尝试(成功/失败/被锁定) */
@Data
@TableName("t_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String ip;
    /** 1成功 0失败 */
    private Integer success;
    /** 结果描述:登录成功/密码错误/账号锁定/验证码错误... */
    private String msg;
    /** 失败时的剩余锁定秒数,前端据此展示倒计时 */
    private Long lockSeconds;
    private LocalDateTime createTime;
}

package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户/会员
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    /** 序列化时不返回给前端 */
    @JsonIgnore
    private String password;
    private String nickname;
    /** 1男 2女 */
    private Integer gender;
    private Integer birthYear;
    private String city;
    /** 联系说明 */
    private String aboutMe;
    private String avatar;
    private String phone;
    /** USER寻亲者 / ADMIN管理员 */
    private String role;
    /** 0待审核 1正常 2未通过 3已停用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 计算属性:年龄 */
    public Integer getAge() {
        return birthYear == null ? null : java.time.Year.now().getValue() - birthYear;
    }
}

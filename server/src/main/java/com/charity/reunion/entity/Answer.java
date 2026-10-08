package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 寻亲问答-回复 */
@Data
@TableName("t_answer")
public class Answer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long userId;
    private String content;
    /** 0正常 1已屏蔽 */
    private Integer status;
    private LocalDateTime createTime;

    /** 非数据库字段:回答人昵称 */
    @TableField(exist = false)
    private String userName;
}

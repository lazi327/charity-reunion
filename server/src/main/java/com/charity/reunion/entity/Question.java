package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 寻亲问答-问题 */
@Data
@TableName("t_question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    /** 0正常 1已屏蔽 */
    private Integer status;
    private Integer viewCount;
    private LocalDateTime createTime;

    /** 非数据库字段:提问人昵称 */
    @TableField(exist = false)
    private String userName;
    /** 非数据库字段:回复数 */
    @TableField(exist = false)
    private Long answerCount;
    /** 非数据库字段:点赞数 */
    @TableField(exist = false)
    private Integer voteCount;
    /** 非数据库字段:当前用户是否已赞 */
    @TableField(exist = false)
    private Boolean voted;
}

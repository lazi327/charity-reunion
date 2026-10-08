package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 寻亲指南(富文本) */
@Data
@TableName("t_guide")
public class Guide {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String cover;
    /** 富文本HTML */
    private String content;
    /** 1发布 0下架 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

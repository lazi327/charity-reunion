package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内留言 */
@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromId;
    private Long toId;
    private String content;
    /** 接收方是否已读 0/1 */
    private Integer readFlag;
    private LocalDateTime createTime;
}

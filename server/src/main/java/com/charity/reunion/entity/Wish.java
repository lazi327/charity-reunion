package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 为寻亲档案点亮一盏回家灯(一人一档案只能点一次,再点取消) */
@Data
@TableName("t_wish")
public class Wish {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long caseId;
    private LocalDateTime createTime;
}

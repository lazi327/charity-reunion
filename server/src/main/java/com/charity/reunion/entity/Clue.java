package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 寻亲线索 */
@Data
@TableName("t_clue")
public class Clue {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long caseId;
    /** 线索提供人 */
    private Long userId;
    private String content;
    /** 提供人联络方式(档案发布者可见) */
    private String contact;
    /** 0待处理 1有效 2虚假 */
    private Integer status;
    private LocalDateTime createTime;

    /** 非数据库字段:提供人昵称 */
    @TableField(exist = false)
    private String userName;
    /** 非数据库字段:关联档案标题(失踪人姓名) */
    @TableField(exist = false)
    private String caseName;
}

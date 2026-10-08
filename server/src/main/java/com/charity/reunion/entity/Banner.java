package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 首页轮播图 */
@Data
@TableName("t_banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String image;
    /** 跳转地址(站内路径或外链) */
    private String linkUrl;
    private Integer sortOrder;
    /** 1上架 0下架 */
    private Integer status;
    private LocalDateTime createTime;
}

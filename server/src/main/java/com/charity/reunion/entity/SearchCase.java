package com.charity.reunion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 寻亲信息 */
@Data
@TableName("t_case")
public class SearchCase {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 发布寻亲者 */
    private Long userId;
    /** 寻亲类型:寻子女/寻父母/寻配偶/寻其他亲属 */
    private String relation;
    /** 失踪人姓名 */
    private String name;
    /** 失踪人性别 1男 2女 */
    private Integer gender;
    private Integer birthYear;
    private String photo;
    /** 失踪地区(省 市) */
    private String lostPlace;
    private LocalDate lostDate;
    /** 体貌特征 */
    private String features;
    /** 失踪经过 */
    private String story;
    private String contactPhone;
    /** 1管理员置顶 */
    private Integer topFlag;
    /** 0待审核 1已发布 2驳回 */
    private Integer status;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 非数据库字段:发布人昵称 */
    @TableField(exist = false)
    private String publisherName;
    /** 非数据库字段:AI/规则匹配得分(0-100) */
    @TableField(exist = false)
    private Integer matchScore;
    /** 非数据库字段:匹配理由 */
    @TableField(exist = false)
    private String matchReason;
    /** 非数据库字段:回家灯点亮数 */
    @TableField(exist = false)
    private Integer wishCount;
    /** 非数据库字段:当前用户是否已点灯 */
    @TableField(exist = false)
    private Boolean wished;
}

package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.Guide;
import com.charity.reunion.mapper.GuideMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 寻亲指南:列表 / 详情(登录后可访问,管理端接口在 AdminController)
 */
@RestController
@RequestMapping("/api/guides")
public class GuideController {

    private final GuideMapper guideMapper;

    public GuideController(GuideMapper guideMapper) {
        this.guideMapper = guideMapper;
    }

    /** 指南列表(不返回正文,减轻传输) */
    @GetMapping
    public Result<List<Guide>> list() {
        List<Guide> list = guideMapper.selectList(Wrappers.<Guide>lambdaQuery()
                .eq(Guide::getStatus, 1)
                .select(Guide::getId, Guide::getTitle, Guide::getCover, Guide::getCreateTime)
                .orderByDesc(Guide::getId));
        return Result.ok(list);
    }

    /** 指南详情(富文本) */
    @GetMapping("/{id}")
    public Result<Guide> detail(@PathVariable Long id) {
        Guide guide = guideMapper.selectById(id);
        if (guide == null || guide.getStatus() != 1) {
            throw new BizException("指南不存在或已下架");
        }
        return Result.ok(guide);
    }
}

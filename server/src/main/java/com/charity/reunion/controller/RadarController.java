package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.SearchCase;
import com.charity.reunion.entity.User;
import com.charity.reunion.mapper.CaseMapper;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.service.FeatureExtractor;
import com.charity.reunion.service.MatchEngine;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 亲缘雷达:自研五维匹配引擎(替代大模型方案)
 * - POST /api/radar/extract   从自然语言描述抽取结构化特征(本地NLP-lite)
 * - POST /api/radar/match     特征向量 → 全库匹配 TopN,返回得分分解与理由
 * - GET  /api/radar/similar/{id}  档案维度的相似档案推荐(排除自身)
 * - GET  /api/radar/lexicon   特征词典(前端标签选择器数据源)
 */
@RestController
@RequestMapping("/api/radar")
public class RadarController {

    private final CaseMapper caseMapper;
    private final UserMapper userMapper;

    public RadarController(CaseMapper caseMapper, UserMapper userMapper) {
        this.caseMapper = caseMapper;
        this.userMapper = userMapper;
    }

    /** 雷达检索表单:表单字段与自由描述二选一或混合,混合时以表单为准 */
    public record RadarForm(String freeText, String nameHint, Integer gender, Integer birthYear,
                            Integer lostYear, String province, List<String> tags) { }

    /** 一键抽取:把大白话描述变成结构化特征 */
    @PostMapping("/extract")
    public Result<Map<String, Object>> extract(@RequestBody Map<String, String> body) {
        FeatureExtractor.Extracted ex = FeatureExtractor.extract(body.get("text"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gender", ex.gender());
        data.put("birthYear", ex.birthYear());
        data.put("lostYear", ex.lostYear());
        data.put("province", ex.province());
        data.put("tags", ex.tags());
        data.put("nameHint", ex.nameHint());
        data.put("empty", ex.isEmpty());
        return Result.ok(data);
    }

    /** 全库匹配 */
    @PostMapping("/match")
    public Result<Map<String, Object>> match(@RequestBody RadarForm form) {
        MatchEngine.QueryVector vec = buildVector(form);
        boolean nothing = vec.nameHint() == null && vec.gender() == null && vec.birthYear() == null
                && vec.lostYear() == null && vec.province() == null && vec.tags().isEmpty();
        if (nothing) {
            throw new BizException("请至少提供一项特征(姓名/性别/年龄/失踪地/失踪年份/体貌标签)");
        }
        List<SearchCase> all = caseMapper.selectList(
                Wrappers.<SearchCase>lambdaQuery().eq(SearchCase::getStatus, 1));
        List<MatchEngine.Hit> hits = new ArrayList<>();
        for (SearchCase c : all) {
            MatchEngine.Hit hit = MatchEngine.score(c, vec);
            if (hit.score() >= 20) {
                hits.add(hit);
            }
        }
        hits.sort(Comparator.comparingInt(MatchEngine.Hit::score).reversed());
        return Result.ok(wrap(hits.stream().limit(10).toList(), vec));
    }

    /** 相似档案推荐:以该档案自身特征为查询向量 */
    @GetMapping("/similar/{id}")
    public Result<Map<String, Object>> similar(@PathVariable Long id) {
        SearchCase base = caseMapper.selectById(id);
        if (base == null || base.getStatus() != 1) {
            throw new BizException("寻亲信息不存在");
        }
        MatchEngine.QueryVector vec = new MatchEngine.QueryVector(
                base.getName(), base.getGender(), base.getBirthYear(),
                base.getLostDate() == null ? null : base.getLostDate().getYear(),
                FeatureExtractor.provinceOf(base.getLostPlace()),
                FeatureExtractor.tagsOf(base.getFeatures(), base.getStory()));
        List<MatchEngine.Hit> hits = new ArrayList<>();
        for (SearchCase c : caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                .eq(SearchCase::getStatus, 1).ne(SearchCase::getId, id))) {
            MatchEngine.Hit hit = MatchEngine.score(c, vec);
            if (hit.score() >= 30) {
                hits.add(hit);
            }
        }
        hits.sort(Comparator.comparingInt(MatchEngine.Hit::score).reversed());
        return Result.ok(wrap(hits.stream().limit(4).toList(), vec));
    }

    /** 特征词典:供前端"体貌标签"多选 */
    @GetMapping("/lexicon")
    public Result<List<String>> lexicon() {
        return Result.ok(List.of("高个子", "矮个子", "圆脸", "瓜子脸", "黑皮肤", "左撇子", "疤痕", "痣",
                "龅牙缺牙", "戴眼镜", "口吃", "听力障碍", "智力障碍", "方言口音", "跛脚步态", "文身", "断指", "走失于车站"));
    }

    /* ---------- 私有工具 ---------- */

    private MatchEngine.QueryVector buildVector(RadarForm form) {
        FeatureExtractor.Extracted ex = FeatureExtractor.extract(form.freeText());
        // 合并:表单显式字段优先,自由文本抽取兜底;标签取并集
        Set<String> tags = new LinkedHashSet<>();
        if (ex.tags() != null) {
            tags.addAll(ex.tags());
        }
        if (form.tags() != null) {
            tags.addAll(form.tags());
        }
        String province = form.province() != null && !form.province().isBlank()
                ? form.province() : ex.province();
        Integer gender = form.gender() != null ? form.gender() : ex.gender();
        Integer birthYear = form.birthYear() != null ? form.birthYear() : ex.birthYear();
        Integer lostYear = form.lostYear() != null ? form.lostYear() : ex.lostYear();
        String nameHint = form.nameHint() != null && !form.nameHint().isBlank()
                ? form.nameHint() : ex.nameHint();
        return new MatchEngine.QueryVector(nameHint, gender, birthYear, lostYear, province, new ArrayList<>(tags));
    }

    private Map<String, Object> wrap(List<MatchEngine.Hit> hits, MatchEngine.QueryVector vec) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (MatchEngine.Hit h : hits) {
            SearchCase c = h.caseRow();
            c.setPublisherName(publisherName(c.getUserId()));
            c.setMatchScore(h.score());
            c.setMatchReason(String.join(";", h.reasons()));
            Map<String, Object> item = new HashMap<>();
            item.put("case", c);
            item.put("score", h.score());
            item.put("dims", h.dims());
            item.put("reasons", h.reasons());
            candidates.add(item);
        }
        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("nameHint", vec.nameHint());
        extracted.put("gender", vec.gender());
        extracted.put("birthYear", vec.birthYear());
        extracted.put("lostYear", vec.lostYear());
        extracted.put("province", vec.province());
        extracted.put("tags", vec.tags());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("engine", "自研五维匹配引擎 v2(编辑距离/高斯年龄/省份邻接图/Jaccard/时间衰减)");
        data.put("extracted", extracted);
        data.put("candidates", candidates);
        return data;
    }

    private String publisherName(Long userId) {
        User publisher = userMapper.selectById(userId);
        return publisher == null ? "已注销用户" : publisher.getNickname();
    }
}

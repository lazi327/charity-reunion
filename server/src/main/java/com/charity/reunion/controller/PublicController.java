package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.Banner;
import com.charity.reunion.entity.Clue;
import com.charity.reunion.entity.SearchCase;
import com.charity.reunion.entity.User;
import com.charity.reunion.entity.Wish;
import com.charity.reunion.mapper.BannerMapper;
import com.charity.reunion.mapper.CaseMapper;
import com.charity.reunion.mapper.ClueMapper;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.mapper.WishMapper;
import com.charity.reunion.service.FeatureExtractor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 免登录公开接口:首页轮播图 / 最新寻亲信息 / 平台数据 / 地区热力 / 寻亲接力动态
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final BannerMapper bannerMapper;
    private final CaseMapper caseMapper;
    private final WishMapper wishMapper;
    private final ClueMapper clueMapper;
    private final UserMapper userMapper;

    public PublicController(BannerMapper bannerMapper, CaseMapper caseMapper, WishMapper wishMapper,
                            ClueMapper clueMapper, UserMapper userMapper) {
        this.bannerMapper = bannerMapper;
        this.caseMapper = caseMapper;
        this.wishMapper = wishMapper;
        this.clueMapper = clueMapper;
        this.userMapper = userMapper;
    }

    /** 首页轮播图 */
    @GetMapping("/banners")
    public Result<List<Banner>> banners() {
        return Result.ok(bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSortOrder)));
    }

    /** 最新发布的寻亲信息(前 8 条,置顶优先) */
    @GetMapping("/latest-cases")
    public Result<List<SearchCase>> latestCases() {
        return Result.ok(caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                .eq(SearchCase::getStatus, 1)
                .select(SearchCase::getId, SearchCase::getRelation, SearchCase::getName,
                        SearchCase::getGender, SearchCase::getPhoto, SearchCase::getLostPlace,
                        SearchCase::getLostDate, SearchCase::getViewCount, SearchCase::getCreateTime)
                .orderByDesc(SearchCase::getTopFlag)
                .orderByDesc(SearchCase::getId)
                .last("limit 8")));
    }

    /** 平台公开统计数字(登录页/首页展示) */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> data = new HashMap<>();
        data.put("caseCount", caseMapper.selectCount(Wrappers.<SearchCase>lambdaQuery().eq(SearchCase::getStatus, 1)));
        data.put("viewTotal", caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                        .eq(SearchCase::getStatus, 1).select(SearchCase::getViewCount))
                .stream().mapToInt(c -> c.getViewCount() == null ? 0 : c.getViewCount()).sum());
        data.put("wishTotal", wishMapper.selectCount(null));
        data.put("clueTotal", clueMapper.selectCount(null));
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        data.put("newCases7d", caseMapper.selectCount(Wrappers.<SearchCase>lambdaQuery()
                .eq(SearchCase::getStatus, 1).ge(SearchCase::getCreateTime, weekAgo)));
        data.put("activeUsers", userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, "USER").ge(User::getCreateTime, weekAgo)));
        return Result.ok(data);
    }

    /** 失踪地区省份热力 Top8(首页条形热度榜) */
    @GetMapping("/region-heat")
    public Result<List<Map<String, Object>>> regionHeat() {
        Map<String, Integer> counter = new TreeMap<>();
        for (SearchCase c : caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                .eq(SearchCase::getStatus, 1).select(SearchCase::getLostPlace))) {
            String p = FeatureExtractor.provinceOf(c.getLostPlace());
            if (p != null) {
                counter.merge(p, 1, Integer::sum);
            }
        }
        List<Map<String, Object>> list = counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                }).toList();
        return Result.ok(list);
    }

    /** 寻亲接力动态:最近点灯/新线索/新档案合并成时间倒序流水(首页跑马灯) */
    @GetMapping("/feed")
    public Result<List<Map<String, Object>>> feed() {
        List<Map<String, Object>> feed = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (Wish w : wishMapper.selectList(Wrappers.<Wish>lambdaQuery().orderByDesc(Wish::getId).last("limit 6"))) {
            SearchCase c = caseMapper.selectById(w.getCaseId());
            if (c == null) {
                continue;
            }
            feed.add(feedItem("lamp", nickname(w.getUserId()) + " 为「" + c.getName() + "」点亮了一盏回家灯",
                    w.getCreateTime(), fmt));
        }
        for (Clue cl : clueMapper.selectList(Wrappers.<Clue>lambdaQuery().orderByDesc(Clue::getId).last("limit 5"))) {
            SearchCase c = caseMapper.selectById(cl.getCaseId());
            if (c == null) {
                continue;
            }
            feed.add(feedItem("clue", nickname(cl.getUserId()) + " 提供了「" + c.getName() + "」的一条线索",
                    cl.getCreateTime(), fmt));
        }
        for (SearchCase c : caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                .eq(SearchCase::getStatus, 1).orderByDesc(SearchCase::getId).last("limit 5"))) {
            feed.add(feedItem("publish", "新档案「" + c.getName() + "·" + c.getRelation() + "」已发布",
                    c.getCreateTime(), fmt));
        }
        feed.sort(Comparator.comparing(m -> (String) m.get("time"), Comparator.reverseOrder()));
        return Result.ok(feed.size() > 12 ? feed.subList(0, 12) : feed);
    }

    private Map<String, Object> feedItem(String icon, String text, LocalDateTime time, DateTimeFormatter fmt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("icon", icon);
        m.put("text", text);
        m.put("time", time == null ? "" : time.format(fmt));
        return m;
    }

    private String nickname(Long userId) {
        User u = userMapper.selectById(userId);
        return u == null ? "好心人" : u.getNickname();
    }
}

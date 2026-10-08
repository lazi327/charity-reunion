package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.*;
import com.charity.reunion.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端(需 ADMIN 角色,由 TokenInterceptor 校验)
 * 轮播图 / 用户启停 / 寻亲信息审核与置顶 / 线索管理 / 指南管理 / 问答管理 / 统计分析
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final List<String> PROVINCES = List.of(
            "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江",
            "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南",
            "湖北", "湖南", "广东", "广西", "海南", "重庆", "四川", "贵州",
            "云南", "西藏", "陕西", "甘肃", "青海", "宁夏", "新疆");

    private final UserMapper userMapper;
    private final BannerMapper bannerMapper;
    private final CaseMapper caseMapper;
    private final ClueMapper clueMapper;
    private final GuideMapper guideMapper;
    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;
    private final MessageMapper messageMapper;
    private final LoginLogMapper loginLogMapper;

    public AdminController(UserMapper userMapper, BannerMapper bannerMapper, CaseMapper caseMapper,
                           ClueMapper clueMapper, GuideMapper guideMapper, QuestionMapper questionMapper,
                           AnswerMapper answerMapper, MessageMapper messageMapper, LoginLogMapper loginLogMapper) {
        this.userMapper = userMapper;
        this.bannerMapper = bannerMapper;
        this.caseMapper = caseMapper;
        this.clueMapper = clueMapper;
        this.guideMapper = guideMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.messageMapper = messageMapper;
        this.loginLogMapper = loginLogMapper;
    }

    // ==================== 仪表盘统计 ====================

    /** 登录安全日志(分页,可按用户名/成败筛选) */
    @GetMapping("/login-logs")
    public Result<Page<LoginLog>> loginLogs(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer success) {
        return Result.ok(loginLogMapper.selectPage(new Page<>(page, size),
                Wrappers.<LoginLog>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), LoginLog::getUsername, keyword)
                        .eq(success != null, LoginLog::getSuccess, success)
                        .orderByDesc(LoginLog::getId)));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getRole, "USER")));
        data.put("pendingUsers", userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, "USER").eq(User::getStatus, 0)));
        data.put("totalCases", caseMapper.selectCount(null));
        data.put("pendingCases", caseMapper.selectCount(Wrappers.<SearchCase>lambdaQuery().eq(SearchCase::getStatus, 0)));
        data.put("publishedCases", caseMapper.selectCount(Wrappers.<SearchCase>lambdaQuery().eq(SearchCase::getStatus, 1)));
        data.put("totalClues", clueMapper.selectCount(null));
        data.put("pendingClues", clueMapper.selectCount(Wrappers.<Clue>lambdaQuery().eq(Clue::getStatus, 0)));
        data.put("totalGuides", guideMapper.selectCount(null));
        data.put("totalQuestions", questionMapper.selectCount(null));
        data.put("totalAnswers", answerMapper.selectCount(null));
        data.put("totalMessages", messageMapper.selectCount(null));
        return Result.ok(data);
    }

    /** 图表数据:失踪地区分布 / 线索提供趋势(近12月) / 寻亲类型分布 */
    @GetMapping("/charts")
    public Result<Map<String, Object>> charts() {
        List<SearchCase> cases = caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                .select(SearchCase::getId, SearchCase::getLostPlace, SearchCase::getRelation, SearchCase::getCreateTime));

        // 失踪人口地区分布(按省份归并)
        Map<String, Integer> regionMap = new LinkedHashMap<>();
        for (SearchCase c : cases) {
            String place = c.getLostPlace() == null ? "未知" : "其他";
            for (String p : PROVINCES) {
                if (c.getLostPlace() != null && c.getLostPlace().contains(p)) {
                    place = p;
                    break;
                }
            }
            regionMap.merge(place, 1, Integer::sum);
        }

        // 寻亲信息类型分布
        Map<String, Integer> typeMap = new LinkedHashMap<>();
        for (SearchCase c : cases) {
            typeMap.merge(c.getRelation() == null ? "其他" : c.getRelation(), 1, Integer::sum);
        }

        // 线索提供趋势:近 12 个月
        List<Clue> clues = clueMapper.selectList(Wrappers.<Clue>lambdaQuery().select(Clue::getCreateTime));
        Map<String, Integer> monthMap = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            monthMap.put(now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")), 0);
        }
        for (Clue cl : clues) {
            LocalDateTime t = cl.getCreateTime();
            if (t == null) {
                continue;
            }
            String key = YearMonth.from(t).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            monthMap.merge(key, 1, Integer::sum);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("regionDist", regionMap);
        data.put("typeDist", typeMap);
        data.put("clueTrend", monthMap);
        return Result.ok(data);
    }

    // ==================== 轮播图管理 ====================

    @GetMapping("/banners")
    public Result<List<Banner>> banners() {
        return Result.ok(bannerMapper.selectList(
                Wrappers.<Banner>lambdaQuery().orderByAsc(Banner::getSortOrder).orderByDesc(Banner::getId)));
    }

    @PostMapping("/banners")
    public Result<Void> addBanner(@RequestBody Banner banner) {
        if (banner.getTitle() == null || banner.getTitle().isBlank()) {
            throw new BizException("请填写轮播图标题");
        }
        if (banner.getImage() == null || banner.getImage().isBlank()) {
            throw new BizException("请上传轮播图片");
        }
        banner.setId(null);
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        bannerMapper.insert(banner);
        return Result.ok();
    }

    @PutMapping("/banners/{id}")
    public Result<Void> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        banner.setId(id);
        bannerMapper.updateById(banner);
        return Result.ok();
    }

    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        bannerMapper.deleteById(id);
        return Result.ok();
    }

    // ==================== 用户管理(启停) ====================

    @GetMapping("/users")
    public Result<Page<User>> users(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) String keyword) {
        Page<User> result = userMapper.selectPage(new Page<>(page, size),
                Wrappers.<User>lambdaQuery()
                        .eq(User::getRole, "USER")
                        .eq(status != null, User::getStatus, status)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(User::getNickname, keyword).or().like(User::getUsername, keyword))
                        .orderByDesc(User::getId));
        return Result.ok(result);
    }

    /** 启用 / 停用违规账号 */
    @PutMapping("/users/{id}/status")
    public Result<Void> toggleUser(@PathVariable Long id, @RequestParam Integer status) {
        if (status != 1 && status != 3) {
            throw new BizException("状态参数错误");
        }
        User user = userMapper.selectById(id);
        if (user == null || "ADMIN".equals(user.getRole())) {
            throw new BizException("用户不存在");
        }
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        return Result.ok();
    }

    /** 审核新用户:status=1 通过 / 2 不通过 */
    @PutMapping("/users/{id}/audit")
    public Result<Void> auditUser(@PathVariable Long id, @RequestParam Integer status) {
        if (status != 1 && status != 2) {
            throw new BizException("审核状态参数错误");
        }
        User user = userMapper.selectById(id);
        if (user == null || "ADMIN".equals(user.getRole())) {
            throw new BizException("用户不存在");
        }
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        return Result.ok();
    }

    // ==================== 寻亲信息管理(审核/置顶/驳回) ====================

    @GetMapping("/cases")
    public Result<Page<SearchCase>> cases(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        Page<SearchCase> result = caseMapper.selectPage(new Page<>(page, size),
                Wrappers.<SearchCase>lambdaQuery()
                        .eq(status != null, SearchCase::getStatus, status)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(SearchCase::getName, keyword).or().like(SearchCase::getLostPlace, keyword))
                        .orderByDesc(SearchCase::getId));
        result.getRecords().forEach(c -> {
            User u = userMapper.selectById(c.getUserId());
            c.setPublisherName(u == null ? "已注销用户" : u.getNickname());
        });
        return Result.ok(result);
    }

    /** 审核寻亲信息:status=1 通过发布 / 2 驳回 */
    @PutMapping("/cases/{id}/audit")
    public Result<Void> auditCase(@PathVariable Long id, @RequestParam Integer status) {
        if (status != 1 && status != 2) {
            throw new BizException("审核状态参数错误");
        }
        SearchCase c = caseMapper.selectById(id);
        if (c == null) {
            throw new BizException("寻亲信息不存在");
        }
        SearchCase update = new SearchCase();
        update.setId(id);
        update.setStatus(status);
        caseMapper.updateById(update);
        return Result.ok();
    }

    /** 置顶 / 取消置顶 */
    @PutMapping("/cases/{id}/top")
    public Result<Void> topCase(@PathVariable Long id, @RequestParam boolean top) {
        SearchCase update = new SearchCase();
        update.setId(id);
        update.setTopFlag(top ? 1 : 0);
        caseMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/cases/{id}")
    public Result<Void> deleteCase(@PathVariable Long id) {
        caseMapper.deleteById(id);
        clueMapper.delete(Wrappers.<Clue>lambdaQuery().eq(Clue::getCaseId, id));
        return Result.ok();
    }

    // ==================== 寻亲线索管理(删除虚假线索) ====================

    @GetMapping("/clues")
    public Result<Page<Clue>> clues(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) Integer status) {
        Page<Clue> result = clueMapper.selectPage(new Page<>(page, size),
                Wrappers.<Clue>lambdaQuery()
                        .eq(status != null, Clue::getStatus, status)
                        .orderByDesc(Clue::getId));
        for (Clue clue : result.getRecords()) {
            User u = userMapper.selectById(clue.getUserId());
            clue.setUserName(u == null ? "已注销用户" : u.getNickname());
            SearchCase c = caseMapper.selectById(clue.getCaseId());
            clue.setCaseName(c == null ? "已删除" : c.getName());
        }
        return Result.ok(result);
    }

    @DeleteMapping("/clues/{id}")
    public Result<Void> deleteClue(@PathVariable Long id) {
        clueMapper.deleteById(id);
        return Result.ok();
    }

    // ==================== 寻亲指南管理(富文本) ====================

    @GetMapping("/guides")
    public Result<List<Guide>> guides() {
        return Result.ok(guideMapper.selectList(
                Wrappers.<Guide>lambdaQuery().orderByDesc(Guide::getId)));
    }

    @PostMapping("/guides")
    public Result<Void> addGuide(@RequestBody Guide guide) {
        if (guide.getTitle() == null || guide.getTitle().isBlank()) {
            throw new BizException("请填写指南标题");
        }
        if (guide.getContent() == null || guide.getContent().isBlank()
                || guide.getContent().replaceAll("<[^>]+>", "").isBlank()) {
            throw new BizException("请填写指南内容");
        }
        guide.setId(null);
        if (guide.getStatus() == null) {
            guide.setStatus(1);
        }
        guideMapper.insert(guide);
        return Result.ok();
    }

    @PutMapping("/guides/{id}")
    public Result<Void> updateGuide(@PathVariable Long id, @RequestBody Guide guide) {
        guide.setId(id);
        guideMapper.updateById(guide);
        return Result.ok();
    }

    @DeleteMapping("/guides/{id}")
    public Result<Void> deleteGuide(@PathVariable Long id) {
        guideMapper.deleteById(id);
        return Result.ok();
    }

    // ==================== 寻亲问答管理(屏蔽不合规内容) ====================

    @GetMapping("/questions")
    public Result<Page<Question>> questions(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Integer status) {
        Page<Question> result = questionMapper.selectPage(new Page<>(page, size),
                Wrappers.<Question>lambdaQuery()
                        .eq(status != null, Question::getStatus, status)
                        .orderByDesc(Question::getId));
        for (Question q : result.getRecords()) {
            User u = userMapper.selectById(q.getUserId());
            q.setUserName(u == null ? "已注销用户" : u.getNickname());
            q.setAnswerCount(answerMapper.selectCount(Wrappers.<Answer>lambdaQuery()
                    .eq(Answer::getQuestionId, q.getId())));
        }
        return Result.ok(result);
    }

    /** 屏蔽 / 恢复问题 */
    @PutMapping("/questions/{id}/status")
    public Result<Void> toggleQuestion(@PathVariable Long id, @RequestParam Integer status) {
        Question update = new Question();
        update.setId(id);
        update.setStatus(status == 1 ? 1 : 0);
        questionMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        questionMapper.deleteById(id);
        answerMapper.delete(Wrappers.<Answer>lambdaQuery().eq(Answer::getQuestionId, id));
        return Result.ok();
    }

    /** 某问题的全部回复(含已屏蔽) */
    @GetMapping("/questions/{id}/answers")
    public Result<List<Answer>> answers(@PathVariable Long id) {
        List<Answer> list = answerMapper.selectList(Wrappers.<Answer>lambdaQuery()
                .eq(Answer::getQuestionId, id).orderByAsc(Answer::getId));
        list.forEach(a -> {
            User u = userMapper.selectById(a.getUserId());
            a.setUserName(u == null ? "已注销用户" : u.getNickname());
        });
        return Result.ok(list);
    }

    /** 屏蔽 / 恢复回复 */
    @PutMapping("/answers/{id}/status")
    public Result<Void> toggleAnswer(@PathVariable Long id, @RequestParam Integer status) {
        Answer update = new Answer();
        update.setId(id);
        update.setStatus(status == 1 ? 1 : 0);
        answerMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/answers/{id}")
    public Result<Void> deleteAnswer(@PathVariable Long id) {
        answerMapper.deleteById(id);
        return Result.ok();
    }
}

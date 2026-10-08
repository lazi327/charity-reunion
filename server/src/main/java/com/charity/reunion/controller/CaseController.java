package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.SearchCase;
import com.charity.reunion.entity.User;
import com.charity.reunion.entity.Wish;
import com.charity.reunion.mapper.CaseMapper;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.mapper.WishMapper;
import com.charity.reunion.security.UserContext;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 寻亲信息:检索大厅 / 详情 / 发布 / 我的 / 点亮回家灯
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseMapper caseMapper;
    private final UserMapper userMapper;
    private final WishMapper wishMapper;

    public CaseController(CaseMapper caseMapper, UserMapper userMapper, WishMapper wishMapper) {
        this.caseMapper = caseMapper;
        this.userMapper = userMapper;
        this.wishMapper = wishMapper;
    }

    /**
     * 寻亲信息检索:地区/关键字/类型/性别/出生年区间 + 排序
     * sort: new最新(默认) / view最受关注 / wish最多点灯 / hot综合热度(浏览+灯数*3+近期加权)
     */
    @GetMapping
    public Result<Page<SearchCase>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "12") long size,
                                         @RequestParam(required = false) String relation,
                                         @RequestParam(required = false) String place,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer gender,
                                         @RequestParam(required = false) Integer minBirthYear,
                                         @RequestParam(required = false) Integer maxBirthYear,
                                         @RequestParam(required = false) Integer minLostYear,
                                         @RequestParam(required = false) Integer maxLostYear,
                                         @RequestParam(defaultValue = "new") String sort) {
        var query = Wrappers.<SearchCase>lambdaQuery()
                .eq(SearchCase::getStatus, 1)
                .and(relation != null && !relation.isBlank() && !"全部".equals(relation),
                        w -> w.eq(SearchCase::getRelation, relation))
                .like(place != null && !place.isBlank(), SearchCase::getLostPlace, place)
                .eq(gender != null, SearchCase::getGender, gender)
                .ge(minBirthYear != null, SearchCase::getBirthYear, minBirthYear)
                .le(maxBirthYear != null, SearchCase::getBirthYear, maxBirthYear)
                .ge(minLostYear != null, SearchCase::getLostDate, minLostYear == null ? null : LocalDate.of(minLostYear, 1, 1))
                .le(maxLostYear != null, SearchCase::getLostDate, maxLostYear == null ? null : LocalDate.of(maxLostYear, 12, 31))
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SearchCase::getName, keyword).or()
                        .like(SearchCase::getFeatures, keyword).or()
                        .like(SearchCase::getLostPlace, keyword));

        Page<SearchCase> result;
        if ("new".equals(sort)) {
            query.orderByDesc(SearchCase::getTopFlag).orderByDesc(SearchCase::getId);
            result = caseMapper.selectPage(new Page<>(page, size), query);
        } else {
            // 热度类排序:全量取出→内存计算→手动分页(档案量级完全可承受)
            List<SearchCase> all = caseMapper.selectList(query);
            fillWishCount(all);
            Comparator<SearchCase> cmp = switch (sort) {
                case "view" -> Comparator.comparing((SearchCase c) -> nz(c.getViewCount())).reversed();
                case "wish" -> Comparator.comparing((SearchCase c) -> nz(c.getWishCount())).reversed();
                default -> Comparator.comparing((SearchCase c) ->
                        nz(c.getViewCount()) + nz(c.getWishCount()) * 3
                                + Math.max(0, 30 - daysSince(c.getCreateTime())) * 2).reversed();
            };
            List<SearchCase> sorted = all.stream().sorted(cmp).toList();
            int from = (int) Math.min((page - 1) * size, sorted.size());
            int to = (int) Math.min(from + size, sorted.size());
            result = new Page<>(page, size, sorted.size());
            result.setRecords(sorted.subList(from, to));
        }
        fillPublisher(result.getRecords());
        fillWishCount(result.getRecords());
        return Result.ok(result);
    }

    /** 我发布的寻亲信息(含各状态) */
    @GetMapping("/my")
    public Result<List<SearchCase>> my() {
        Long me = UserContext.requireId();
        List<SearchCase> list = caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                .eq(SearchCase::getUserId, me).orderByDesc(SearchCase::getId));
        fillWishCount(list);
        return Result.ok(list);
    }

    /** 详情:浏览量+1,返回发布人昵称 */
    @GetMapping("/{id}")
    public Result<SearchCase> detail(@PathVariable Long id) {
        SearchCase c = caseMapper.selectById(id);
        if (c == null) {
            throw new BizException("寻亲信息不存在");
        }
        Long me = UserContext.requireId();
        if (c.getStatus() != 1 && !c.getUserId().equals(me)) {
            throw new BizException("该信息尚未通过审核");
        }
        if (!c.getUserId().equals(me)) {
            SearchCase update = new SearchCase();
            update.setId(id);
            update.setViewCount((c.getViewCount() == null ? 0 : c.getViewCount()) + 1);
            caseMapper.updateById(update);
            c.setViewCount(update.getViewCount());
        }
        fillPublisher(List.of(c));
        fillWishCount(List.of(c));
        c.setWished(wishMapper.selectCount(Wrappers.<Wish>lambdaQuery()
                .eq(Wish::getCaseId, id).eq(Wish::getUserId, me)) > 0);
        return Result.ok(c);
    }

    /** 点亮/取消点亮回家灯(toggle),返回最新状态 */
    @PostMapping("/{id}/wish")
    public Result<Map<String, Object>> wish(@PathVariable Long id) {
        Long me = UserContext.requireId();
        SearchCase c = caseMapper.selectById(id);
        if (c == null || c.getStatus() != 1) {
            throw new BizException("寻亲信息不存在或未发布");
        }
        Wish exist = wishMapper.selectOne(Wrappers.<Wish>lambdaQuery()
                .eq(Wish::getCaseId, id).eq(Wish::getUserId, me).last("limit 1"));
        boolean mine;
        if (exist == null) {
            Wish w = new Wish();
            w.setCaseId(id);
            w.setUserId(me);
            wishMapper.insert(w);
            mine = true;
        } else {
            wishMapper.deleteById(exist.getId());
            mine = false;
        }
        long count = wishMapper.selectCount(Wrappers.<Wish>lambdaQuery().eq(Wish::getCaseId, id));
        Map<String, Object> data = new HashMap<>();
        data.put("wishCount", count);
        data.put("mine", mine);
        return Result.ok(data);
    }

    /** 发布寻亲信息(需审核通过后公开) */
    @PostMapping
    public Result<Long> create(@RequestBody SearchCase form) {
        Long me = UserContext.requireId();
        validate(form);
        form.setId(null);
        form.setUserId(me);
        form.setStatus(0);
        form.setTopFlag(0);
        form.setViewCount(0);
        caseMapper.insert(form);
        return Result.ok(form.getId());
    }

    /** 修改自己的寻亲信息(修改后重新审核) */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SearchCase form) {
        Long me = UserContext.requireId();
        SearchCase old = caseMapper.selectById(id);
        if (old == null || !old.getUserId().equals(me)) {
            throw new BizException("只能修改自己发布的信息");
        }
        validate(form);
        SearchCase update = new SearchCase();
        update.setId(id);
        update.setRelation(form.getRelation());
        update.setName(form.getName());
        update.setGender(form.getGender());
        update.setBirthYear(form.getBirthYear());
        update.setPhoto(form.getPhoto());
        update.setLostPlace(form.getLostPlace());
        update.setLostDate(form.getLostDate());
        update.setFeatures(form.getFeatures());
        update.setStory(form.getStory());
        update.setContactPhone(form.getContactPhone());
        update.setStatus(0);
        caseMapper.updateById(update);
        return Result.ok();
    }

    /** 删除自己发布的信息 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long me = UserContext.requireId();
        SearchCase old = caseMapper.selectById(id);
        if (old == null || !old.getUserId().equals(me)) {
            throw new BizException("只能删除自己发布的信息");
        }
        caseMapper.deleteById(id);
        return Result.ok();
    }

    /** 失踪地区字典(供检索下拉) */
    @GetMapping("/places")
    public Result<List<String>> places() {
        List<String> places = caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                        .eq(SearchCase::getStatus, 1).select(SearchCase::getLostPlace))
                .stream().map(SearchCase::getLostPlace).distinct().sorted().toList();
        return Result.ok(places);
    }

    private void validate(SearchCase form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new BizException("请填写失踪人姓名");
        }
        if (form.getRelation() == null || form.getRelation().isBlank()) {
            throw new BizException("请选择寻亲类型");
        }
        if (form.getGender() == null || (form.getGender() != 1 && form.getGender() != 2)) {
            throw new BizException("请选择失踪人性别");
        }
        if (form.getLostPlace() == null || form.getLostPlace().isBlank()) {
            throw new BizException("请填写失踪地区");
        }
        if (form.getFeatures() == null || form.getFeatures().isBlank()) {
            throw new BizException("请填写体貌特征,便于他人辨认");
        }
        LocalDate date = form.getLostDate();
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BizException("失踪日期不能晚于今天");
        }
    }

    private void fillPublisher(List<SearchCase> list) {
        for (SearchCase c : list) {
            User publisher = userMapper.selectById(c.getUserId());
            c.setPublisherName(publisher == null ? "已注销用户" : publisher.getNickname());
        }
    }

    /** 批量填充灯数与我的点灯状态(一次查询,避免 N+1) */
    private void fillWishCount(List<SearchCase> list) {
        if (list.isEmpty()) {
            return;
        }
        Set<Long> ids = list.stream().map(SearchCase::getId).collect(Collectors.toSet());
        List<Wish> wishes = wishMapper.selectList(Wrappers.<Wish>lambdaQuery().in(Wish::getCaseId, ids));
        Long me = UserContext.getId();
        Map<Long, Long> counter = wishes.stream()
                .collect(Collectors.groupingBy(Wish::getCaseId, Collectors.counting()));
        for (SearchCase c : list) {
            c.setWishCount(counter.getOrDefault(c.getId(), 0L).intValue());
            c.setWished(me != null && wishes.stream().anyMatch(
                    w -> w.getCaseId().equals(c.getId()) && w.getUserId().equals(me)));
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static int daysSince(java.time.LocalDateTime t) {
        return t == null ? 999 : (int) java.time.temporal.ChronoUnit.DAYS.between(t.toLocalDate(), LocalDate.now());
    }
}

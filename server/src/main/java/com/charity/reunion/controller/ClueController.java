package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.Clue;
import com.charity.reunion.entity.SearchCase;
import com.charity.reunion.entity.User;
import com.charity.reunion.mapper.CaseMapper;
import com.charity.reunion.mapper.ClueMapper;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.security.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 寻亲线索:提供 / 我的线索 / 收到的线索 / 处理
 */
@RestController
@RequestMapping("/api/clues")
public class ClueController {

    private final ClueMapper clueMapper;
    private final CaseMapper caseMapper;
    private final UserMapper userMapper;

    public ClueController(ClueMapper clueMapper, CaseMapper caseMapper, UserMapper userMapper) {
        this.clueMapper = clueMapper;
        this.caseMapper = caseMapper;
        this.userMapper = userMapper;
    }

    /** 向某条寻亲信息提供线索 */
    @PostMapping
    public Result<Void> submit(@RequestBody Clue form) {
        Long me = UserContext.requireId();
        if (form.getCaseId() == null) {
            throw new BizException("缺少寻亲信息ID");
        }
        if (form.getContent() == null || form.getContent().trim().length() < 5) {
            throw new BizException("线索内容至少 5 个字,请尽量描述具体时间地点");
        }
        SearchCase c = caseMapper.selectById(form.getCaseId());
        if (c == null || c.getStatus() != 1) {
            throw new BizException("该寻亲信息不存在或未发布");
        }
        if (c.getUserId().equals(me)) {
            throw new BizException("不能给自己的发布的信息提供线索");
        }
        Clue clue = new Clue();
        clue.setCaseId(form.getCaseId());
        clue.setUserId(me);
        clue.setContent(form.getContent().trim());
        clue.setContact(form.getContact());
        clue.setStatus(0);
        clueMapper.insert(clue);
        return Result.ok();
    }

    /** 我提供的线索 */
    @GetMapping("/my")
    public Result<List<Clue>> my() {
        Long me = UserContext.requireId();
        List<Clue> clues = clueMapper.selectList(Wrappers.<Clue>lambdaQuery()
                .eq(Clue::getUserId, me).orderByDesc(Clue::getId));
        clues.forEach(c -> {
            SearchCase caseItem = caseMapper.selectById(c.getCaseId());
            c.setCaseName(caseItem == null ? "已删除" : caseItem.getName());
        });
        return Result.ok(clues);
    }

    /** 我发布的寻亲信息收到的线索(仅档案发布者可见提供人联络方式) */
    @GetMapping("/received")
    public Result<List<Clue>> received() {
        Long me = UserContext.requireId();
        List<Long> caseIds = caseMapper.selectList(Wrappers.<SearchCase>lambdaQuery()
                        .eq(SearchCase::getUserId, me).select(SearchCase::getId))
                .stream().map(SearchCase::getId).toList();
        if (caseIds.isEmpty()) {
            return Result.ok(List.of());
        }
        List<Clue> clues = clueMapper.selectList(Wrappers.<Clue>lambdaQuery()
                .in(Clue::getCaseId, caseIds).orderByDesc(Clue::getId));
        fillExtra(clues);
        return Result.ok(clues);
    }

    /** 某条信息下的线索列表:公开仅显示内容摘要,发布者本人可见联络方式 */
    @GetMapping("/case/{caseId}")
    public Result<List<Clue>> byCase(@PathVariable Long caseId) {
        Long me = UserContext.requireId();
        SearchCase c = caseMapper.selectById(caseId);
        if (c == null) {
            throw new BizException("寻亲信息不存在");
        }
        List<Clue> clues = clueMapper.selectList(Wrappers.<Clue>lambdaQuery()
                .eq(Clue::getCaseId, caseId).orderByDesc(Clue::getId));
        if (!c.getUserId().equals(me)) {
            // 非发布者:隐藏联络方式与处理状态
            clues = clues.stream().peek(x -> {
                x.setContact(null);
                x.setStatus(null);
            }).toList();
        }
        fillExtra(new ArrayList<>(clues));
        return Result.ok(clues);
    }

    /** 档案发布者标记线索:1有效 2虚假 */
    @PutMapping("/{id}/status")
    public Result<Void> mark(@PathVariable Long id, @RequestParam Integer status) {
        if (status != 1 && status != 2) {
            throw new BizException("参数错误");
        }
        Long me = UserContext.requireId();
        Clue clue = clueMapper.selectById(id);
        if (clue == null) {
            throw new BizException("线索不存在");
        }
        SearchCase c = caseMapper.selectById(clue.getCaseId());
        if (c == null || !c.getUserId().equals(me)) {
            throw new BizException("只有档案发布者可以处理线索");
        }
        Clue update = new Clue();
        update.setId(id);
        update.setStatus(status);
        clueMapper.updateById(update);
        return Result.ok();
    }

    private void fillExtra(List<Clue> clues) {
        for (Clue clue : clues) {
            User u = userMapper.selectById(clue.getUserId());
            clue.setUserName(u == null ? "已注销用户" : u.getNickname());
            SearchCase caseItem = caseMapper.selectById(clue.getCaseId());
            clue.setCaseName(caseItem == null ? "已删除" : caseItem.getName());
        }
    }
}

package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.Answer;
import com.charity.reunion.entity.Question;
import com.charity.reunion.entity.User;
import com.charity.reunion.entity.Vote;
import com.charity.reunion.mapper.AnswerMapper;
import com.charity.reunion.mapper.QuestionMapper;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.mapper.VoteMapper;
import com.charity.reunion.security.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 寻亲问答:提问 / 列表 / 详情 / 回答 / 点赞
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;
    private final UserMapper userMapper;
    private final VoteMapper voteMapper;

    public QuestionController(QuestionMapper questionMapper, AnswerMapper answerMapper,
                              UserMapper userMapper, VoteMapper voteMapper) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.userMapper = userMapper;
        this.voteMapper = voteMapper;
    }

    /** 问答列表:关键字搜索 + 排序(new最新/hot热度=赞*3+回复*5+浏览) */
    @GetMapping
    public Result<Page<Question>> list(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "new") String sort) {
        var query = Wrappers.<Question>lambdaQuery()
                .eq(Question::getStatus, 0)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Question::getTitle, keyword).or().like(Question::getContent, keyword));
        Page<Question> result;
        if ("hot".equals(sort)) {
            List<Question> all = questionMapper.selectList(query);
            all.forEach(this::fillExtra);
            List<Question> sorted = all.stream().sorted(Comparator.comparing(
                    (Question q) -> nz(q.getVoteCount()) * 3 + nz(q.getAnswerCount().intValue()) * 5
                            + nz(q.getViewCount())).reversed()).toList();
            int from = (int) Math.min((page - 1) * size, sorted.size());
            int to = (int) Math.min(from + size, sorted.size());
            result = new Page<>(page, size, sorted.size());
            result.setRecords(sorted.subList(from, to));
        } else {
            result = questionMapper.selectPage(new Page<>(page, size), query.orderByDesc(Question::getId));
            result.getRecords().forEach(this::fillExtra);
        }
        return Result.ok(result);
    }

    /** 我提的问题 */
    @GetMapping("/my")
    public Result<List<Question>> my() {
        Long me = UserContext.requireId();
        List<Question> list = questionMapper.selectList(Wrappers.<Question>lambdaQuery()
                .eq(Question::getUserId, me).orderByDesc(Question::getId));
        return Result.ok(list);
    }

    /** 问题详情 + 回复列表 */
    @GetMapping("/{id}")
    public Result<Question> detail(@PathVariable Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BizException("问题不存在");
        }
        Long me = UserContext.requireId();
        if (q.getStatus() == 1 && !q.getUserId().equals(me)) {
            throw new BizException("该问题已被屏蔽");
        }
        if (!q.getUserId().equals(me)) {
            Question update = new Question();
            update.setId(id);
            update.setViewCount((q.getViewCount() == null ? 0 : q.getViewCount()) + 1);
            questionMapper.updateById(update);
            q.setViewCount(update.getViewCount());
        }
        fillExtra(q);
        return Result.ok(q);
    }

    /** 某问题的正常回复列表 */
    @GetMapping("/{id}/answers")
    public Result<List<Answer>> answers(@PathVariable Long id) {
        List<Answer> list = answerMapper.selectList(Wrappers.<Answer>lambdaQuery()
                .eq(Answer::getQuestionId, id)
                .eq(Answer::getStatus, 0)
                .orderByAsc(Answer::getId));
        list.forEach(a -> {
            User u = userMapper.selectById(a.getUserId());
            a.setUserName(u == null ? "已注销用户" : u.getNickname());
        });
        return Result.ok(list);
    }

    /** 发布提问 */
    @PostMapping
    public Result<Long> create(@RequestBody Question form) {
        Long me = UserContext.requireId();
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            throw new BizException("请填写问题标题");
        }
        if (form.getContent() == null || form.getContent().trim().length() < 10) {
            throw new BizException("问题描述至少 10 个字,便于他人理解和解答");
        }
        Question q = new Question();
        q.setUserId(me);
        q.setTitle(form.getTitle().trim());
        q.setContent(form.getContent().trim());
        q.setStatus(0);
        q.setViewCount(0);
        questionMapper.insert(q);
        return Result.ok(q.getId());
    }

    /** 邀请其他用户作答:回答问题 */
    @PostMapping("/{id}/answers")
    public Result<Void> answer(@PathVariable Long id, @RequestBody Answer form) {
        Long me = UserContext.requireId();
        Question q = questionMapper.selectById(id);
        if (q == null || q.getStatus() == 1) {
            throw new BizException("问题不存在或已被屏蔽");
        }
        if (form.getContent() == null || form.getContent().trim().length() < 5) {
            throw new BizException("回复至少 5 个字");
        }
        Answer a = new Answer();
        a.setQuestionId(id);
        a.setUserId(me);
        a.setContent(form.getContent().trim());
        a.setStatus(0);
        answerMapper.insert(a);
        return Result.ok();
    }

    /** 点赞/取消点赞(toggle) */
    @PostMapping("/{id}/vote")
    public Result<Map<String, Object>> vote(@PathVariable Long id) {
        Long me = UserContext.requireId();
        Question q = questionMapper.selectById(id);
        if (q == null || q.getStatus() == 1) {
            throw new BizException("问题不存在或已被屏蔽");
        }
        Vote exist = voteMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Vote>lambdaQuery()
                .eq(Vote::getQuestionId, id).eq(Vote::getUserId, me).last("limit 1"));
        boolean mine;
        if (exist == null) {
            Vote v = new Vote();
            v.setQuestionId(id);
            v.setUserId(me);
            voteMapper.insert(v);
            mine = true;
        } else {
            voteMapper.deleteById(exist.getId());
            mine = false;
        }
        long count = voteMapper.selectCount(Wrappers.<Vote>lambdaQuery().eq(Vote::getQuestionId, id));
        Map<String, Object> data = new HashMap<>();
        data.put("voteCount", count);
        data.put("mine", mine);
        return Result.ok(data);
    }

    private void fillExtra(Question q) {
        User u = userMapper.selectById(q.getUserId());
        q.setUserName(u == null ? "已注销用户" : u.getNickname());
        q.setAnswerCount(answerMapper.selectCount(Wrappers.<Answer>lambdaQuery()
                .eq(Answer::getQuestionId, q.getId()).eq(Answer::getStatus, 0)));
        q.setVoteCount(voteMapper.selectCount(Wrappers.<Vote>lambdaQuery()
                .eq(Vote::getQuestionId, q.getId())).intValue());
        Long me = UserContext.getId();
        q.setVoted(me != null && voteMapper.selectCount(Wrappers.<Vote>lambdaQuery()
                .eq(Vote::getQuestionId, q.getId()).eq(Vote::getUserId, me)) > 0);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}

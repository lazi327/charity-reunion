package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.Message;
import com.charity.reunion.entity.User;
import com.charity.reunion.mapper.MessageMapper;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.security.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息(会话列表 / 聊天记录 / 发送),管理员与寻亲者均可使用
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    public MessageController(MessageMapper messageMapper, UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    /** 会话列表:每个对方最近一条消息 + 未读数 */
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        Long me = UserContext.requireId();
        List<Message> recent = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .and(w -> w.eq(Message::getFromId, me).or().eq(Message::getToId, me))
                .orderByDesc(Message::getId)
                .last("limit 500"));

        // 按对方分组保留最近一条
        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Message m : recent) {
            Long peerId = m.getFromId().equals(me) ? m.getToId() : m.getFromId();
            Map<String, Object> item = grouped.get(peerId);
            if (item == null) {
                item = new HashMap<>();
                item.put("peerId", peerId);
                item.put("lastContent", m.getContent());
                item.put("lastTime", m.getCreateTime());
                item.put("unread", 0);
                grouped.put(peerId, item);
            }
            if (m.getToId().equals(me) && m.getReadFlag() == 0) {
                item.put("unread", (Integer) item.get("unread") + 1);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : grouped.values()) {
            User peer = userMapper.selectById((Long) item.get("peerId"));
            if (peer != null) {
                item.put("peer", peer);
                list.add(item);
            }
        }
        return Result.ok(list);
    }

    /** 未读消息总数 */
    @GetMapping("/unread")
    public Result<Long> unread() {
        Long me = UserContext.requireId();
        return Result.ok(messageMapper.selectCount(Wrappers.<Message>lambdaQuery()
                .eq(Message::getToId, me).eq(Message::getReadFlag, 0)));
    }

    /** 与某人的聊天记录(自动标记已读) */
    @GetMapping("/{peerId}")
    public Result<List<Message>> chat(@PathVariable Long peerId) {
        Long me = UserContext.requireId();
        List<Message> msgs = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .and(w -> w.eq(Message::getFromId, me).eq(Message::getToId, peerId)
                        .or().eq(Message::getFromId, peerId).eq(Message::getToId, me))
                .orderByAsc(Message::getId)
                .last("limit 200"));

        Message mark = new Message();
        mark.setReadFlag(1);
        messageMapper.update(mark, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getToId, me).eq(Message::getFromId, peerId).eq(Message::getReadFlag, 0));
        return Result.ok(msgs);
    }

    /** 发送消息 */
    @PostMapping
    public Result<Message> send(@RequestBody Map<String, Object> body) {
        Long me = UserContext.requireId();
        Long toId = body.get("toId") == null ? null : Long.valueOf(body.get("toId").toString());
        String content = body.get("content") == null ? null : body.get("content").toString().trim();
        if (toId == null || content == null || content.isEmpty()) {
            throw new BizException("参数不完整");
        }
        if (toId.equals(me)) {
            throw new BizException("不能给自己发消息");
        }
        User target = userMapper.selectById(toId);
        if (target == null || target.getStatus() != 1) {
            throw new BizException("接收方不存在或账号已停用");
        }
        if (content.length() > 1000) {
            throw new BizException("消息过长");
        }
        Message msg = new Message();
        msg.setFromId(me);
        msg.setToId(toId);
        msg.setContent(content);
        msg.setReadFlag(0);
        messageMapper.insert(msg);
        return Result.ok(msg);
    }
}

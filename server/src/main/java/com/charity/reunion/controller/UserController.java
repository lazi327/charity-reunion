package com.charity.reunion.controller;

import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.entity.User;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.security.UserContext;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心:我的资料 / 查看其他用户公开资料
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 我的资料 */
    @GetMapping("/me")
    public Result<User> me() {
        return Result.ok(userMapper.selectById(UserContext.requireId()));
    }

    /** 修改我的资料(仅允许常用字段) */
    @PutMapping("/me")
    public Result<Void> updateMe(@RequestBody User form) {
        Long id = UserContext.requireId();
        User update = new User();
        update.setId(id);
        update.setNickname(form.getNickname());
        update.setGender(form.getGender());
        update.setBirthYear(form.getBirthYear());
        update.setCity(form.getCity());
        update.setAboutMe(form.getAboutMe());
        update.setAvatar(form.getAvatar());
        update.setPhone(form.getPhone());
        userMapper.updateById(update);
        return Result.ok();
    }

    /** 查看其他用户公开资料(问答/线索中的用户名跳转) */
    @GetMapping("/{id}")
    public Result<User> detail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null || "ADMIN".equals(user.getRole())) {
            throw new BizException("用户不存在");
        }
        if (user.getStatus() != 1 && !user.getId().equals(UserContext.requireId())) {
            throw new BizException("该用户尚未通过审核");
        }
        user.setPhone(null); // 手机号仅本人可见,沟通请使用站内消息
        return Result.ok(user);
    }
}

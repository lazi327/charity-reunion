package com.charity.reunion.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import com.charity.reunion.dto.ForgotPasswordDTO;
import com.charity.reunion.dto.LoginDTO;
import com.charity.reunion.dto.RegisterDTO;
import com.charity.reunion.entity.LoginLog;
import com.charity.reunion.entity.User;
import com.charity.reunion.mapper.LoginLogMapper;
import com.charity.reunion.mapper.UserMapper;
import com.charity.reunion.security.CaptchaUtil;
import com.charity.reunion.security.JwtUtil;
import com.charity.reunion.security.LoginSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 注册 / 登录 / 图形验证码 / 找回密码
 * 登录带防爆破:连续失败 5 次锁定 5 分钟,所有尝试写入登录日志
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 特殊响应码:账号被锁定,前端据此展示倒计时 */
    private static final int CODE_LOCKED = 2;

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginSecurityService securityService;

    public AuthController(UserMapper userMapper, LoginLogMapper loginLogMapper,
                          PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                          LoginSecurityService securityService) {
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.securityService = securityService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().length() < 3) {
            throw new BizException("账号至少 3 个字符");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        if (dto.getNickname() == null || dto.getNickname().isBlank()) {
            throw new BizException("请填写昵称");
        }
        if (dto.getGender() == null || (dto.getGender() != 1 && dto.getGender() != 2)) {
            throw new BizException("请选择性别");
        }
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (exists > 0) {
            throw new BizException("该账号已被注册");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setStatus(1);   // 注册后默认正常,管理员可停用违规账号
        userMapper.insert(user);
        return Result.ok();
    }

    /** 注册页实时查重:返回 true 表示可用 */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        if (username == null || username.length() < 3) {
            return Result.ok(false);
        }
        Long n = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username.trim()));
        return Result.ok(n == 0);
    }

    /** 获取算术图形验证码(base64 图片,5 分钟有效,一次性) */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        String[] pair = CaptchaUtil.create();
        Map<String, String> data = new HashMap<>();
        data.put("id", pair[0]);
        data.put("img", pair[1]);
        return Result.ok(data);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (username.isEmpty() || dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new BizException("请输入账号和密码");
        }

        // 1. 锁定检查
        long locked = securityService.lockedSeconds(username);
        if (locked > 0) {
            writeLog(username, request, 0, "账号锁定期内尝试登录", locked);
            return lockedResult("账号已锁定,请 " + locked + " 秒后再试", locked);
        }

        // 2. 验证码检查(错误不计入账号锁定,但一次性作废防重放)
        if (!CaptchaUtil.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            writeLog(username, request, 0, "验证码错误或已过期", null);
            throw new BizException("验证码错误或已过期");
        }

        // 3. 凭据检查
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            long lockLeft = securityService.recordFail(username);
            int remain = Math.max(0, 5 - securityService.failCount(username));
            if (lockLeft > 0) {
                writeLog(username, request, 0, "密码错误达到上限,账号锁定", lockLeft);
                return lockedResult("连续输错 5 次,账号已锁定 " + lockLeft + " 秒", lockLeft);
            }
            writeLog(username, request, 0, "账号或密码错误", null);
            throw new BizException("账号或密码错误,还可尝试 " + remain + " 次");
        }

        // 4. 账号状态检查
        if ("USER".equals(user.getRole())) {
            switch (user.getStatus()) {
                case 0 -> {
                    writeLog(username, request, 0, "账号待审核", null);
                    throw new BizException("注册申请正在审核中,通过后即可登录");
                }
                case 2 -> {
                    writeLog(username, request, 0, "审核未通过", null);
                    throw new BizException("资料审核未通过,请修改资料后联系管理员重新提交");
                }
                case 3 -> {
                    writeLog(username, request, 0, "账号被停用", null);
                    throw new BizException("账号已被停用,请联系管理员");
                }
                default -> { }
            }
        }

        // 5. 成功:清零失败计数并签发 token
        securityService.clear(username);
        writeLog(username, request, 1, "登录成功", null);
        String token = jwtUtil.createToken(user.getId(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.ok(data);
    }

    /** 找回密码:账号 + 注册手机号匹配通过后,重置为默认密码 123456 */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody ForgotPasswordDTO dto, HttpServletRequest request) {
        if (dto.getUsername() == null || dto.getPhone() == null) {
            throw new BizException("请填写账号和注册手机号");
        }
        if (!CaptchaUtil.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BizException("验证码错误或已过期");
        }
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername().trim()));
        // 统一话术,不泄露账号是否存在
        if (user == null || !dto.getPhone().equals(user.getPhone())) {
            writeLog(dto.getUsername(), request, 0, "找回密码身份核验失败", null);
            throw new BizException("账号与注册手机号不匹配,请联系管理员协助处理");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        securityService.clear(user.getUsername());
        writeLog(user.getUsername(), request, 1, "通过手机号核验重置密码", null);
        return Result.ok();
    }

    private Result<Map<String, Object>> lockedResult(String msg, long seconds) {
        Result<Map<String, Object>> r = new Result<>();
        r.setCode(CODE_LOCKED);
        r.setMsg(msg);
        Map<String, Object> data = new HashMap<>();
        data.put("lockSeconds", seconds);
        r.setData(data);
        return r;
    }

    private void writeLog(String username, HttpServletRequest request, int success, String msg, Long lockSeconds) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setIp(clientIp(request));
        log.setSuccess(success);
        log.setMsg(msg);
        log.setLockSeconds(lockSeconds);
        loginLogMapper.insert(log);
    }

    /** 取真实客户端 IP(兼容反向代理) */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        return (ip != null && !ip.isBlank()) ? ip : request.getRemoteAddr();
    }
}
